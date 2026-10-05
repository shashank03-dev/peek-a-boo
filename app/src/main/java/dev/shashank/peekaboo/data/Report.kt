package dev.shashank.peekaboo.data

import dev.shashank.peekaboo.detect.FaceSignature
import java.util.Calendar

data class DayReport(
    val peeks: Int,
    val people: Int,
    val totalPeekMs: Long,
    val longestPeekMs: Long,
    val peakHour: Int?,
    val hourly: IntArray,
    val lastPeekAt: Long?,
)

data class DailyCount(val dayStart: Long, val count: Int)

object Reports {
    fun startOfDay(time: Long = System.currentTimeMillis(), daysAgo: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = time
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun hourOf(time: Long): Int = Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.HOUR_OF_DAY)

    fun day(all: List<PeekEvent>, faces: List<PeekFace>, threshold: Float?): DayReport {
        val events = all.filter { !it.isStranger }
        val hourly = IntArray(24)
        events.forEach { hourly[hourOf(it.startedAt)]++ }
        val peak = hourly.indices.maxByOrNull { hourly[it] }?.takeIf { hourly[it] > 0 }
        val people = uniquePeople(faces, threshold, fallback = events.sumOf { it.maxPeepers })
        return DayReport(
            peeks = events.size,
            people = people,
            totalPeekMs = events.sumOf { it.durationMs },
            longestPeekMs = events.maxOfOrNull { it.durationMs } ?: 0,
            peakHour = peak,
            hourly = hourly,
            lastPeekAt = events.maxOfOrNull { it.startedAt },
        )
    }

    /** Greedy clustering of peeper signatures: faces closer than the threshold are treated as the same person. */
    private fun uniquePeople(faces: List<PeekFace>, threshold: Float?, fallback: Int): Int {
        if (faces.isEmpty()) return fallback
        val limit = (threshold ?: FaceSignature.DEFAULT_THRESHOLD) * 1.1f
        val centers = mutableListOf<FloatArray>()
        for (f in faces) {
            val sig = FaceSignature.fromBytes(f.signature) ?: continue
            if (centers.none { FaceSignature.distance(it, sig) <= limit }) centers += sig
        }
        return centers.size.coerceAtLeast(if (faces.isNotEmpty()) 1 else 0)
    }

    fun week(events: List<PeekEvent>, now: Long = System.currentTimeMillis()): List<DailyCount> =
        (6 downTo 0).map { ago ->
            val start = startOfDay(now, ago)
            val end = startOfDay(now, ago - 1)
            DailyCount(start, events.count { !it.isStranger && it.startedAt in start until end })
        }

    fun formatDuration(ms: Long): String {
        val s = ms / 1000
        return when {
            s < 1 -> "<1s"
            s < 60 -> "${s}s"
            s < 3600 -> "${s / 60}m ${s % 60}s"
            else -> "${s / 3600}h ${(s % 3600) / 60}m"
        }
    }

    fun formatHour(h: Int): String = when {
        h == 0 -> "12 AM"
        h < 12 -> "$h AM"
        h == 12 -> "12 PM"
        else -> "${h - 12} PM"
    }
}
