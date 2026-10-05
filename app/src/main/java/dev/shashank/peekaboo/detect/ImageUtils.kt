package dev.shashank.peekaboo.detect

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

/** Converts a camera frame to an upright bitmap so face boxes line up with what people see. */
fun ImageProxy.toUprightBitmap(): Bitmap {
    val bmp = toBitmap()
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return bmp
    val m = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true).also { if (it !== bmp) bmp.recycle() }
}

/** Suspends until CameraX's process-wide camera provider is ready. */
suspend fun android.content.Context.cameraProvider(): androidx.camera.lifecycle.ProcessCameraProvider =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val future = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this)
        future.addListener(
            { runCatching { future.get() }.onSuccess { cont.resumeWith(Result.success(it)) }.onFailure { cont.resumeWith(Result.failure(it)) } },
            androidx.core.content.ContextCompat.getMainExecutor(this),
        )
    }
