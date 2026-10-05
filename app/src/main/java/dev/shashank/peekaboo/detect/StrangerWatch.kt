package dev.shashank.peekaboo.detect

/**
 * Notices when someone other than the enrolled owner is the one using the unlocked phone: the
 * person holding it (the closest face) is looking at the screen and isn't you, steadily, for a
 * few seconds, while your face is nowhere in view.
 *
 * Fires at most once per stretch; seeing the owner again, or the phone locking, re-arms it.
 */
class StrangerWatch(
    private val holdMs: Long = DEFAULT_HOLD_MS,
    /** Short gaps (a blink, a missed frame) don't reset the clock. */
    private val graceMs: Long = 1_500,
) {
    private var since: Long? = null
    private var lastSeen = 0L
    private var fired = false

    /**
     * @param holderIsStranger the closest face is looking at the screen and isn't the owner.
     * @return true exactly once when a stranger has been using the phone for [holdMs].
     */
    fun onFrame(now: Long, ownerInView: Boolean, holderIsStranger: Boolean): Boolean {
        if (ownerInView) {
            reset()
            return false
        }
        if (!holderIsStranger) {
            if (since != null && now - lastSeen > graceMs) since = null
            return false
        }
        if (since == null) since = now
        lastSeen = now
        if (!fired && now - since!! >= holdMs) {
            fired = true
            return true
        }
        return false
    }

    fun reset() {
        since = null
        fired = false
    }

    companion object {
        const val DEFAULT_HOLD_MS = 6_000L
    }
}
