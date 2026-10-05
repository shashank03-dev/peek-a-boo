package dev.shashank.peekaboo.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Live, process-wide state of the guard, observed by the UI, the overlay and the tile. */
object GuardState {
    /** Service is alive and holding its foreground notification. */
    val running = MutableStateFlow(false)
    /** Camera is actually scanning (screen on and unlocked). */
    val scanning = MutableStateFlow(false)
    val facesInView = MutableStateFlow(0)
    val ownerInView = MutableStateFlow(false)
    val peepersNow = MutableStateFlow(0)
    val peekActive = MutableStateFlow(false)
    /** UI screens that need the front camera themselves (Face ID enrollment) take a lease. */
    val uiCameraLeases = MutableStateFlow(0)

    fun acquireCamera() = uiCameraLeases.update { it + 1 }
    fun releaseCamera() = uiCameraLeases.update { (it - 1).coerceAtLeast(0) }
}
