package dev.shashank.peekaboo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.shashank.peekaboo.data.PeekEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSettings
import java.io.File
import java.time.Duration

/**
 * Walks through the whole app on Robolectric (a simulated Android in the JVM) the way a new user
 * would, asserting each screen shows up and saving a screenshot of it to app/build/tour/.
 *
 * The camera and ML Kit don't exist here, so this covers UI, navigation, storage and Pro gating,
 * not live face detection.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class AppTourTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<PeekApp>()
    private val out = File("build/tour").apply { mkdirs() }

    @Test
    fun tour() {
        seedHistory()
        compose.mainClock.autoAdvance = false
        ActivityScenario.launch(MainActivity::class.java).use {
            settle()
            // Onboarding: welcome, then skip each permission step.
            expect("Get started")
            shot("01-onboarding")
            click("Get started")
            repeat(4) { click("Not now") }

            // Home ("Watch") with today's seeded peeks.
            expect("Turn it on to catch shoulder surfers.")
            settle(1500)
            shot("02-home")

            click("Activity")
            expect("Peeks today")
            shot("03-activity")
            scrollTo("Someone else used your phone")
            compose.onAllNodesWithText("Someone else used your phone")[0].performClick()
            settle(800)
            expect("Delete this entry")
            shot("04-someone-else-detail")
            it.onActivity { a -> a.onBackPressedDispatcher.onBackPressed() }
            settle(800)

            click("You")
            expect("Teach it your face")
            shot("05-face")

            click("Tune")
            // Sideload builds (-PunlockPro=true) start as Pro, so there's no free tier to check.
            if (!BuildConfig.PRO_UNLOCKED) {
                expect("Get Peek-a-Boo Pro")
                shot("06-tune-free")
                scrollTo("PRIVACY SHIELD")
                shot("07-tune-shield-free")

                // A free user tapping a Pro control lands on the paywall. There's no Play Store
                // here, so it should say so instead of spinning forever.
                click("Protected apps", scroll = true)
                expect("Stop them reading, not just catch them")
                // Play never answers here: after the connect timeout the paywall must stop spinning.
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9))
                expect("Pro isn't available on this install")
                shot("08-paywall-no-play")
                it.onActivity { a -> a.onBackPressedDispatcher.onBackPressed() }
                settle(800)
            }

            // Unlock Pro the way a cached Play purchase would.
            runBlocking { app.settings.setProCached(true) }
            settle()
            scrollTo("PRIVACY SHIELD")
            shot("09-tune-shield-pro")
            click("Protected apps", scroll = true)
            expect("Only in protected apps")
            shot("10-protected-apps")
            it.onActivity { a -> a.onBackPressedDispatcher.onBackPressed() }
            settle(800)

            // Try the shield: the overlay window should appear over the app, then go away.
            ShadowSettings.setCanDrawOverlays(true)
            click("Try it for 5 seconds", scroll = true)
            settle(600)
            assertTrue("shield overlay not shown", overlayTitles().contains("PeekShield"))
            screen("11-shield-preview")
            settle(6000)
            assertTrue("shield overlay stuck", !overlayTitles().contains("PeekShield"))

            // Leaving Tune mid-preview must still take the shield down.
            click("Watch")
            click("Activity")
            click("Tune")
            click("Try it for 5 seconds", scroll = true)
            settle(300)
            click("Watch")
            settle(6000)
            assertTrue("shield stuck after leaving Tune", !overlayTitles().contains("PeekShield"))
            shot("12-home-after")
        }
    }

    private fun seedHistory() = runBlocking {
        val dao = app.db.dao()
        val now = System.currentTimeMillis()
        dao.insert(PeekEvent(startedAt = now - 3 * 3600_000, endedAt = now - 3 * 3600_000 + 4200, maxPeepers = 1))
        dao.insert(PeekEvent(startedAt = now - 2 * 3600_000, endedAt = now - 2 * 3600_000 + 9100, maxPeepers = 2))
        dao.insert(PeekEvent(startedAt = now - 40 * 60_000, endedAt = now - 40 * 60_000 + 6000, maxPeepers = 1, kind = PeekEvent.KIND_STRANGER))
        dao.insert(PeekEvent(startedAt = now - 10 * 60_000, endedAt = now - 10 * 60_000 + 2500, maxPeepers = 1))
    }

    private fun settle(ms: Long = 1200) {
        var left = ms
        while (left > 0) {
            compose.mainClock.advanceTimeBy(minOf(left, 100))
            shadowOf(Looper.getMainLooper()).idle()
            left -= 100
        }
    }

    private fun expect(text: String) {
        try {
            compose.waitUntil(5000) {
                settle(100)
                compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            screen("zz-failed-waiting-for-${text.replace(Regex("[^A-Za-z0-9]+"), "-")}")
            throw AssertionError("\"$text\" never appeared", e)
        }
    }

    private fun click(text: String, scroll: Boolean = false) {
        expect(text)
        if (scroll) scrollTo(text)
        compose.onAllNodesWithText(text)[0].performClick()
        settle()
    }

    /**
     * Swipes the scrollable screen until [text] is comfortably on screen. performScrollTo() can't
     * be used: it waits for frames, and this test drives the frame clock by hand.
     */
    private fun scrollTo(text: String) {
        repeat(12) {
            val node = compose.onAllNodesWithText(text).fetchSemanticsNodes().firstOrNull()
            val screenH = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().firstOrNull()?.boundsInRoot?.bottom ?: 0f
            if (node != null && node.boundsInRoot.top > 0f && node.boundsInRoot.bottom < screenH * 0.8f) return
            compose.onAllNodes(hasScrollAction())[0].performTouchInput {
                swipe(Offset(centerX, bottom * 0.7f), Offset(centerX, bottom * 0.35f), 300)
            }
            settle(700)
        }
    }

    /** Screenshot of the app window. */
    private fun shot(name: String) = screen(name)

    /** Screenshot of every window on screen, overlays included, composited in z-order. */
    private fun screen(name: String) {
        val (views, params) = windows()
        val root = views.firstOrNull() ?: return
        val bmp = Bitmap.createBitmap(root.width.coerceAtLeast(1), root.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.BLACK)
        views.forEachIndexed { i, v ->
            if (v.width == 0 || v.height == 0) return@forEachIndexed
            val layer = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(layer))
            val lp = params.getOrNull(i)
            val paint = Paint().apply { alpha = ((lp?.alpha ?: 1f) * 255).toInt() }
            val loc = IntArray(2).also { v.getLocationOnScreen(it) }
            c.drawBitmap(layer, loc[0].toFloat(), loc[1].toFloat(), paint)
        }
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Suppress("UNCHECKED_CAST", "PrivateApi")
    private fun windows(): Pair<List<View>, List<WindowManager.LayoutParams>> {
        val cls = Class.forName("android.view.WindowManagerGlobal")
        val global = cls.getMethod("getInstance").invoke(null)
        val views = cls.getDeclaredField("mViews").apply { isAccessible = true }.get(global) as List<View>
        val params = cls.getDeclaredField("mParams").apply { isAccessible = true }.get(global) as List<WindowManager.LayoutParams>
        return views.toList() to params.toList()
    }

    private fun overlayTitles() = windows().second.map { it.title.toString() }
}
