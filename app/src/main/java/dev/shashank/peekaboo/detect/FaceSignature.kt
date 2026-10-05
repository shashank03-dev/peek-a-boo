package dev.shashank.peekaboo.detect

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Lightweight, fully on-device face signature: the face is cropped, de-rotated, equalised and
 * described with spatial histograms of uniform Local Binary Patterns (the classic LBPH face
 * recogniser). It needs no downloaded model and runs in well under a millisecond per face.
 */
object FaceSignature {
    private const val SIZE = 66            // 64x64 LBP map after dropping the 1px border
    private const val GRID = 8             // 8x8 cells of 8x8 px
    private const val CELL = 64 / GRID
    private const val BINS = 59            // 58 uniform patterns + 1 "other"
    const val LENGTH = GRID * GRID * BINS
    const val DEFAULT_THRESHOLD = 0.30f

    private val uniform: IntArray = IntArray(256).also { table ->
        var next = 0
        for (code in 0 until 256) {
            var transitions = 0
            for (b in 0 until 8) {
                val a = (code shr b) and 1
                val c = (code shr ((b + 1) % 8)) and 1
                if (a != c) transitions++
            }
            table[code] = if (transitions <= 2) next++ else BINS - 1
        }
    }

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    /** @param rollDeg ML Kit's headEulerAngleZ (positive = counter-clockwise in the image). */
    fun compute(frame: Bitmap, box: Rect, rollDeg: Float): FloatArray {
        val side = max(box.width(), box.height()).toFloat().coerceAtLeast(8f)
        val matrix = Matrix().apply {
            postTranslate(-box.exactCenterX(), -box.exactCenterY())
            postRotate(rollDeg)
            postScale(SIZE / side, SIZE / side)
            postTranslate(SIZE / 2f, SIZE / 2f)
        }
        val crop = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        synchronized(paint) { Canvas(crop).drawBitmap(frame, matrix, paint) }
        val px = IntArray(SIZE * SIZE)
        crop.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        crop.recycle()

        val gray = IntArray(px.size) { i ->
            val c = px[i]
            (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
        }
        equalize(gray)

        val out = FloatArray(LENGTH)
        val norm = 1f / (CELL * CELL)
        for (y in 1 until SIZE - 1) {
            for (x in 1 until SIZE - 1) {
                val c = gray[y * SIZE + x]
                var code = 0
                if (gray[(y - 1) * SIZE + x - 1] >= c) code = code or 1
                if (gray[(y - 1) * SIZE + x] >= c) code = code or 2
                if (gray[(y - 1) * SIZE + x + 1] >= c) code = code or 4
                if (gray[y * SIZE + x + 1] >= c) code = code or 8
                if (gray[(y + 1) * SIZE + x + 1] >= c) code = code or 16
                if (gray[(y + 1) * SIZE + x] >= c) code = code or 32
                if (gray[(y + 1) * SIZE + x - 1] >= c) code = code or 64
                if (gray[y * SIZE + x - 1] >= c) code = code or 128
                val cell = ((y - 1) / CELL) * GRID + (x - 1) / CELL
                out[cell * BINS + uniform[code]] += norm
            }
        }
        return out
    }

    private fun equalize(gray: IntArray) {
        val hist = IntArray(256)
        for (g in gray) hist[g]++
        var acc = 0
        val cdf = IntArray(256) { acc += hist[it]; acc }
        val min = cdf.first { it > 0 }
        val range = (gray.size - min).coerceAtLeast(1)
        for (i in gray.indices) gray[i] = ((cdf[gray[i]] - min) * 255 / range).coerceIn(0, 255)
    }

    /** Chi-square distance normalised to 0..1 (0 = identical). */
    fun distance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val s = a[i] + b[i]
            if (s > 0f) {
                val d = a[i] - b[i]
                sum += d * d / s
            }
        }
        return sum / (2f * GRID * GRID)
    }

    fun minDistance(gallery: List<FloatArray>, sig: FloatArray): Float =
        gallery.minOfOrNull { distance(it, sig) } ?: Float.MAX_VALUE

    /**
     * Picks a match threshold from the owner's own samples: leave-one-out nearest distances
     * describe how much "you" vary; anything well beyond that spread is somebody else.
     */
    fun calibrate(samples: List<FloatArray>): Float {
        if (samples.size < 3) return DEFAULT_THRESHOLD
        val loo = samples.indices.map { i ->
            samples.indices.filter { it != i }.minOf { distance(samples[i], samples[it]) }
        }
        val mean = loo.average().toFloat()
        val std = sqrt(loo.map { (it - mean) * (it - mean) }.average()).toFloat()
        return (mean + 3f * std + 0.04f).coerceIn(0.12f, 0.42f)
    }

    fun toBytes(sig: FloatArray): ByteArray =
        ByteArray(sig.size) { (sig[it] * 255f).roundToInt().coerceIn(0, 255).toByte() }

    fun fromBytes(bytes: ByteArray): FloatArray? =
        if (bytes.size != LENGTH) null else FloatArray(bytes.size) { (bytes[it].toInt() and 0xFF) / 255f }
}
