package dev.shashank.peekaboo.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** Hosts a Compose UI in a system overlay window pinned under the front camera. */
class NotchOverlay(private val context: Context) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    fun canShow() = Settings.canDrawOverlays(context)

    /**
     * Attaches the overlay. The window has a fixed size for its whole life: resizing an overlay
     * window every animation frame forces a system relayout per frame, which is what made the
     * old wrap-content pill stutter and clip its text mid-animation.
     *
     * @param pillTopPx distance from the top of the screen to the top edge of the resting pill.
     * @return true if the window was newly attached.
     */
    fun show(pillTopPx: Int, content: @Composable (NotchSpec) -> Unit): Boolean {
        if (view != null || !canShow()) return false
        val density = context.resources.displayMetrics.density
        val screenW = screenWidthPx()
        val winW = minOf(screenW, (WINDOW_WIDTH_DP * density).toInt())
        val pillTop = pillTopPx.coerceAtLeast((4 * density).toInt())
        val winH = pillTop + (WINDOW_EXTRA_HEIGHT_DP * density).toInt()
        val spec = cameraSpec(screenW, winW, pillTop, density)

        val o = OverlayOwner().also { it.start() }
        val v = ComposeView(context).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setViewTreeViewModelStoreOwner(o)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { content(spec) }
        }
        val lp = WindowManager.LayoutParams(
            winW,
            winH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            title = "PeekNotch"
        }
        return runCatching { wm.addView(v, lp) }.onSuccess {
            view = v
            owner = o
        }.onFailure { o.stop() }.isSuccess
    }

    /**
     * Where the pill drips out of. A centred cut-out (waterdrop or punch-hole) is used as the
     * source; anything else (corner hole, no cut-out) drips from the top edge of the screen.
     */
    private fun cameraSpec(screenW: Int, winW: Int, pillTop: Int, density: Float): NotchSpec {
        val left = (screenW - winW) / 2f
        val cut = topCutout()
        val centred = cut != null && kotlin.math.abs(cut.exactCenterX() - screenW / 2f) < screenW * 0.12f
        return if (cut != null && centred) {
            val r = (minOf(cut.width(), cut.height()) * 0.36f).coerceIn(4 * density, 12 * density)
            NotchSpec(winW, pillTop, cut.exactCenterX() - left, cut.exactCenterY().coerceAtMost(pillTop.toFloat()), r)
        } else {
            NotchSpec(winW, pillTop, winW / 2f, 0f, 8 * density)
        }
    }

    private fun screenWidthPx(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) runCatching {
            return wm.currentWindowMetrics.bounds.width()
        }
        return context.resources.displayMetrics.widthPixels
    }

    private fun topCutout(): Rect? = runCatching {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                wm.currentWindowMetrics.windowInsets.displayCutout?.boundingRectTop
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                @Suppress("DEPRECATION") wm.defaultDisplay.cutout?.boundingRectTop
            else -> null
        }
    }.getOrNull()?.takeIf { !it.isEmpty }

    fun hide() {
        view?.let { runCatching { wm.removeViewImmediate(it) } }
        owner?.stop()
        view = null
        owner = null
    }

    /** Distance in px from the top of the screen to just below the camera cut-out / status bar. */
    fun cameraBottomPx(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) runCatching {
            val insets = wm.currentWindowMetrics.windowInsets
            val cutout = insets.displayCutout
            val top = cutout?.boundingRectTop?.takeIf { !it.isEmpty }?.bottom
            if (top != null && top > 0) return top
            val bars = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top
            if (bars > 0) return bars
        }
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    companion object {
        private const val WINDOW_WIDTH_DP = 340
        /** Pill height plus room for the spring overshoot below it. */
        private const val WINDOW_EXTRA_HEIGHT_DP = 84
    }

    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
        override val viewModelStore = ViewModelStore()

        fun start() {
            savedState.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }
}

/**
 * Geometry handed to the pill, in overlay-window pixels.
 * [cameraX]/[cameraY]/[cameraR] describe the blob the pill drips out of.
 */
data class NotchSpec(
    val windowWidthPx: Int,
    val pillTopPx: Int,
    val cameraX: Float,
    val cameraY: Float,
    val cameraR: Float,
)
