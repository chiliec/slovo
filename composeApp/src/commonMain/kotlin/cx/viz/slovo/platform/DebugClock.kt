package cx.viz.slovo.platform

/**
 * DEBUG-ONLY calendar offset. SRS intervals are whole days, so manual testing
 * needs to fast-forward the clock to see cards fall due. Stays 0 in normal use;
 * the YOU screen exposes a "+1 DAY" button that bumps it.
 */
object DebugClock {
    var dayOffset: Long = 0
}

/** Effective "today" used across the app: the real epoch day plus any debug offset. */
fun currentEpochDay(): Long = todayEpochDay() + DebugClock.dayOffset
