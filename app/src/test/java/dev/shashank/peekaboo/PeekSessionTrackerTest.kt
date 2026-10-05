package dev.shashank.peekaboo

import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.detect.FaceSignature
import dev.shashank.peekaboo.detect.PeekSessionTracker
import dev.shashank.peekaboo.detect.PeekSessionTracker.Change
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeekSessionTrackerTest {
    @Test
    fun brief_glance_is_not_a_peek() {
        val t = PeekSessionTracker(dwellMs = 800)
        assertNull(t.onFrame(0, 1))
        assertNull(t.onFrame(400, 1))
        assertNull(t.onFrame(600, 0))
        assertNull(t.onFrame(5000, 0))
    }

    @Test
    fun sustained_look_starts_updates_and_ends_a_peek() {
        val t = PeekSessionTracker(dwellMs = 800, graceMs = 2500)
        assertNull(t.onFrame(0, 1))
        val started = t.onFrame(900, 1)
        assertEquals(Change.Started(0, 1), started)
        assertEquals(Change.Updated(2), t.onFrame(1200, 2))
        assertNull(t.onFrame(1400, 2))
        assertEquals(Change.Updated(0), t.onFrame(2200, 0))
        assertNull(t.onFrame(3000, 0))
        val ended = t.onFrame(4000, 0)
        assertEquals(Change.Ended(0, 1400, 2), ended)
    }

    @Test
    fun flush_closes_open_session() {
        val t = PeekSessionTracker(dwellMs = 0)
        t.onFrame(10, 1)
        assertTrue(t.active)
        assertEquals(Change.Ended(10, 10, 1), t.flush())
        assertNull(t.flush())
    }

    @Test
    fun signature_bytes_round_trip_and_distance() {
        val a = FloatArray(FaceSignature.LENGTH) { (it % 7) / 7f }
        val back = FaceSignature.fromBytes(FaceSignature.toBytes(a))!!
        assertTrue(FaceSignature.distance(a, back) < 0.01f)
        assertEquals(0f, FaceSignature.distance(a, a), 0f)
    }

    @Test
    fun day_report_counts_peaks() {
        val base = Reports.startOfDay() + 9 * 3_600_000L
        val events = listOf(
            PeekEvent(1, base, base + 4000, 1),
            PeekEvent(2, base + 60_000, base + 61_000, 2),
            PeekEvent(3, base + 5 * 3_600_000L, base + 5 * 3_600_000L + 10_000, 1),
        )
        val r = Reports.day(events, emptyList(), null)
        assertEquals(3, r.peeks)
        assertEquals(4, r.people) // falls back to summed peepers without signatures
        assertEquals(9, r.peakHour)
        assertEquals(10_000, r.longestPeekMs)
        assertEquals(15_000, r.totalPeekMs)
    }
}
