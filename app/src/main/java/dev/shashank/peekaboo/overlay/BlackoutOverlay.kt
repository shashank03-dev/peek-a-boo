package dev.shashank.peekaboo.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.Inter

/**
 * Blackout: the moment someone peeks, the whole screen goes black until they look away or you
 * tap to bring it back. Unlike the shield this window is opaque and takes touches, so a single
 * tap anywhere reveals the screen again; it never traps you.
 */
class BlackoutOverlay(private val context: Context) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private var view: android.view.View? = null
    private var owner: OverlayOwner? = null

    val isShown: Boolean get() = view != null

    fun show(onReveal: () -> Unit) {
        if (view != null || !Settings.canDrawOverlays(context)) return
        val o = OverlayOwner().also { it.start() }
        val v = o.composeView(context) { BlackoutCard(onReveal) }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                setFitInsetsTypes(0)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            title = "PeekBlackout"
        }
        runCatching { wm.addView(v, lp) }
            .onSuccess { view = v; owner = o }
            .onFailure { o.stop() }
    }

    fun hide() {
        view?.let { runCatching { wm.removeViewImmediate(it) } }
        owner?.stop()
        view = null
        owner = null
    }
}

@Composable
private fun BlackoutCard(onReveal: () -> Unit) {
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(120)) }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(Color.Black)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onReveal),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Ink.AlertSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.VisibilityOff, null, tint = Ink.Alert, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Screen hidden",
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Someone was looking. Tap anywhere to show it again.",
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, textAlign = TextAlign.Center),
                color = Ink.TextMuted,
                modifier = Modifier.padding(horizontal = 48.dp),
            )
        }
    }
}
