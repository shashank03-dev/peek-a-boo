package dev.shashank.peekaboo.detect

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dev.shashank.peekaboo.data.OwnerProfile
import dev.shashank.peekaboo.data.Sensitivity
import kotlin.math.abs

data class SeenFace(
    val box: Rect,
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val trackingId: Int?,
    val signature: FloatArray,
    val ownerDistance: Float?,
    val isOwner: Boolean,
    val isLooking: Boolean,
)

data class FrameResult(
    val bitmap: Bitmap,
    val faces: List<SeenFace>,
    val peepers: List<SeenFace>,
    val ownerInView: Boolean,
)

/** Turns an upright front-camera frame into "who is looking at the screen, and who of them isn't you". */
class PeekAnalyzer {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.06f)
            .enableTracking()
            .build()
    )

    /** Rolling owner votes per ML Kit tracking id so a single bad frame doesn't flip identity. */
    private val votes = HashMap<Int, Float>()

    fun detect(bitmap: Bitmap): List<Face> =
        Tasks.await(detector.process(InputImage.fromBitmap(bitmap, 0)))

    fun analyze(
        bitmap: Bitmap,
        owner: OwnerProfile?,
        sensitivity: Sensitivity,
        strict: Boolean,
    ): FrameResult {
        val raw = detect(bitmap)
        val minSide = sensitivity.minFaceFraction * minOf(bitmap.width, bitmap.height)
        val seen = raw.mapNotNull { f ->
            val box = Rect(f.boundingBox).apply {
                intersect(0, 0, bitmap.width, bitmap.height)
            }
            if (box.width() < 8 || box.height() < 8) return@mapNotNull null
            val sig = FaceSignature.compute(bitmap, box, f.headEulerAngleZ)
            val dist = owner?.let { FaceSignature.minDistance(it.samples, sig) }
            val instantOwner = owner != null && dist != null && dist <= owner.threshold
            val id = f.trackingId
            val isOwner = if (id != null && owner != null) {
                val prev = votes[id]
                val v = if (prev == null) (if (instantOwner) 1f else 0f)
                else prev * 0.6f + (if (instantOwner) 0.4f else 0f)
                votes[id] = v
                v >= 0.5f
            } else instantOwner
            SeenFace(
                box = box,
                yaw = f.headEulerAngleY,
                pitch = f.headEulerAngleX,
                roll = f.headEulerAngleZ,
                trackingId = id,
                signature = sig,
                ownerDistance = dist,
                isOwner = isOwner,
                isLooking = box.width() >= minSide &&
                    abs(f.headEulerAngleY) <= sensitivity.maxYawDeg &&
                    abs(f.headEulerAngleX) <= 30f,
            )
        }
        val liveIds = seen.mapNotNull { it.trackingId }.toSet()
        votes.keys.retainAll(liveIds)

        val ownerInView = seen.any { it.isOwner }
        // Whoever is holding the phone is never a peeper. If we can't see a confirmed owner face
        // we assume the closest (largest) face is the person using the phone, unless strict mode
        // says only the enrolled owner may look.
        val holder = when {
            ownerInView -> null
            strict && owner != null -> null
            else -> seen.maxByOrNull { it.box.width() * it.box.height() }
        }
        val peepers = seen.filter { !it.isOwner && it !== holder && it.isLooking }
        return FrameResult(bitmap, seen, peepers, ownerInView)
    }

    fun close() = detector.close()
}
