package dev.shashank.peekaboo.detect

/**
 * Debounces per-frame peeper counts into peek sessions: a peek starts once strangers have been
 * looking for [dwellMs] and ends after [graceMs] without any.
 */
class PeekSessionTracker(var dwellMs: Long, private val graceMs: Long = 2500) {
    sealed interface Change {
        data class Started(val at: Long, val peepers: Int) : Change
        data class Updated(val peepers: Int) : Change
        data class Ended(val startedAt: Long, val endedAt: Long, val maxPeepers: Int) : Change
    }

    private var pendingSince: Long? = null
    private var startedAt: Long? = null
    private var lastSeen = 0L
    private var maxPeepers = 0
    private var current = 0

    val active: Boolean get() = startedAt != null

    fun onFrame(now: Long, peepers: Int): Change? {
        if (peepers > 0) {
            lastSeen = now
            val start = startedAt
            if (start != null) {
                maxPeepers = maxOf(maxPeepers, peepers)
                if (peepers != current) {
                    current = peepers
                    return Change.Updated(peepers)
                }
                return null
            }
            val pending = pendingSince ?: now.also { pendingSince = it }
            if (now - pending >= dwellMs) {
                startedAt = pending
                maxPeepers = peepers
                current = peepers
                pendingSince = null
                return Change.Started(pending, peepers)
            }
            return null
        }
        pendingSince = null
        val start = startedAt ?: return null
        if (current != 0 && now - lastSeen > 700) {
            current = 0
            return Change.Updated(0)
        }
        if (now - lastSeen > graceMs) return finish(start)
        return null
    }

    /** Forces any open session closed, e.g. when the screen turns off. */
    fun flush(): Change.Ended? = startedAt?.let { finish(it) }

    private fun finish(start: Long): Change.Ended {
        val ended = Change.Ended(start, lastSeen.coerceAtLeast(start), maxPeepers)
        startedAt = null
        pendingSince = null
        maxPeepers = 0
        current = 0
        return ended
    }
}
