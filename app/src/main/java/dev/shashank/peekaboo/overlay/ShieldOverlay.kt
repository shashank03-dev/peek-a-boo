package dev.shashank.peekaboo.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.hardware.input.InputManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import dev.shashank.peekaboo.data.ShieldStyle
import kotlin.random.Random

/**
 * Privacy Shield: a full-screen, touch-through filter that makes the screen hard to read for
 * anyone who isn't right in front of it.
 *
 * A phone can't physically narrow its viewing angle the way a Galaxy S26 Ultra's panel does, so
 * the shield works on contrast instead: it darkens the screen and lays a fine pattern over it.
 * Up close your eyes resolve the content between the lines; from a metre away or at an angle the
 * pattern and lost contrast wash text out.
 *
 * Android 12+ only lets touches pass through another app's overlay when the overlay window is at
 * most 80% opaque, so the window alpha is capped at [maxAlpha] and taps keep reaching the app.
 */
class ShieldOverlay(private val context: Context) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private var view: ShieldView? = null
    private var lp: WindowManager.LayoutParams? = null

    val isShown: Boolean get() = view != null

    fun canShow() = Settings.canDrawOverlays(context)

    /** Shows the shield, or restyles it in place if it's already up. */
    fun show(style: ShieldStyle, strengthPercent: Int) {
        val alpha = (strengthPercent / 100f).coerceIn(0.2f, maxAlpha())
        view?.let { v ->
            v.style = style
            lp?.let { p ->
                if (p.alpha != alpha) {
                    p.alpha = alpha
                    runCatching { wm.updateViewLayout(v, p) }
                }
            }
            return
        }
        if (!canShow()) return
        val v = ShieldView(context).apply { this.style = style; this.alpha = 0f }
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.alpha = alpha
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                setFitInsetsTypes(0)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            title = "PeekShield"
        }
        runCatching { wm.addView(v, p) }.onSuccess {
            view = v
            lp = p
            v.animate().alpha(1f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    fun hide(animated: Boolean = true) {
        val v = view ?: return
        view = null
        lp = null
        if (!animated) {
            runCatching { wm.removeViewImmediate(v) }
            return
        }
        v.animate().cancel()
        v.animate().alpha(0f).setDuration(260).withEndAction {
            runCatching { wm.removeViewImmediate(v) }
        }.start()
    }

    private fun maxAlpha(): Float {
        val system = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { context.getSystemService(InputManager::class.java).maximumObscuringOpacityForTouch }.getOrNull() ?: 0.8f
        } else 0.8f
        // Stay just under the limit: at exactly the limit float rounding can still block touches.
        return (system - 0.02f).coerceIn(0.3f, MAX_STRENGTH / 100f)
    }

    companion object {
        const val MIN_STRENGTH = 30
        const val MAX_STRENGTH = 78
    }
}

/** Draws one shield pattern as a repeating tile; the window alpha sets how strong it is. */
private class ShieldView(context: Context) : View(context) {
    private val paint = Paint()
    private val density = context.resources.displayMetrics.density

    var style: ShieldStyle = ShieldStyle.Louver
        set(value) {
            if (field == value && paint.shader != null) return
            field = value
            paint.shader = BitmapShader(tile(value), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            invalidate()
        }

    /**
     * Louver: dark slats with a thin clear gap, like a privacy film seen from the side.
     * Dim: plain black.
     * Grain: dense random speckle that breaks up letter shapes at a distance.
     */
    private fun tile(style: ShieldStyle): Bitmap = when (style) {
        ShieldStyle.Louver -> {
            val slat = (2 * density).toInt().coerceAtLeast(2)
            val gap = (1 * density).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(1, slat + gap, Bitmap.Config.ARGB_8888).apply {
                for (y in 0 until slat) setPixel(0, y, Color.BLACK)
                for (y in slat until slat + gap) setPixel(0, y, Color.argb(90, 0, 0, 0))
            }
        }
        ShieldStyle.Dim -> Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { setPixel(0, 0, Color.BLACK) }
        ShieldStyle.Grain -> {
            val n = 96
            val rnd = Random(7)
            Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until n) for (y in 0 until n) {
                    val a = if (rnd.nextFloat() < 0.72f) 255 else 60 + rnd.nextInt(80)
                    setPixel(x, y, Color.argb(a, 0, 0, 0))
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
