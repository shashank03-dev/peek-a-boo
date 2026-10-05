package dev.shashank.peekaboo

import dev.shashank.peekaboo.billing.ProStore
import dev.shashank.peekaboo.data.PeekEvent
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.detect.StrangerWatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProFeaturesTest {
    @Test
    fun stranger_must_hold_the_phone_for_the_full_stretch() {
        val w = StrangerWatch(holdMs = 6000, graceMs = 1500)
        assertFalse(w.onFrame(0, ownerInView = false, holderIsStranger = true))
        assertFalse(w.onFrame(5000, ownerInView = false, holderIsStranger = true))
        assertTrue(w.onFrame(6000, ownerInView = false, holderIsStranger = true))
        // Fires once per stretch.
        assertFalse(w.onFrame(9000, ownerInView = false, holderIsStranger = true))
    }

    @Test
    fun owner_coming_back_resets_and_rearms() {
        val w = StrangerWatch(holdMs = 6000, graceMs = 1500)
        w.onFrame(0, false, true)
        assertTrue(w.onFrame(6000, false, true))
        assertFalse(w.onFrame(7000, ownerInView = true, holderIsStranger = false))
        assertFalse(w.onFrame(8000, false, true))
        assertTrue(w.onFrame(14000, false, true))
    }

    @Test
    fun short_gaps_are_forgiven_long_ones_are_not() {
        val w = StrangerWatch(holdMs = 6000, graceMs = 1500)
        w.onFrame(0, false, true)
        w.onFrame(2000, false, true)
        w.onFrame(2500, false, false) // a blink / missed frame
        w.onFrame(3000, false, true)
        assertTrue(w.onFrame(6000, false, true))

        val v = StrangerWatch(holdMs = 6000, graceMs = 1500)
        v.onFrame(0, false, true)
        v.onFrame(1000, false, true)
        v.onFrame(4000, false, false) // looked away for 3s
        assertFalse(v.onFrame(6000, false, true))
        assertTrue(v.onFrame(12000, false, true))
    }

    @Test
    fun stranger_events_are_not_counted_as_peeks() {
        val now = Reports.startOfDay() + 12 * 3600_000L
        val events = listOf(
            PeekEvent(id = 1, startedAt = now, endedAt = now + 2000, maxPeepers = 2),
            PeekEvent(id = 2, startedAt = now + 10_000, endedAt = now + 16_000, maxPeepers = 1, kind = PeekEvent.KIND_STRANGER),
        )
        val r = Reports.day(events, emptyList(), null)
        assertEquals(1, r.peeks)
        assertEquals(2000, r.totalPeekMs)
        assertEquals(1, Reports.week(events, now).last().count)
    }

    @Test
    fun billing_periods_convert_to_days() {
        assertEquals(7, ProStore.periodDays("P7D"))
        assertEquals(7, ProStore.periodDays("P1W"))
        assertEquals(30, ProStore.periodDays("P1M"))
        assertEquals(365, ProStore.periodDays("P1Y"))
        assertEquals(0, ProStore.periodDays("garbage"))
    }
}
