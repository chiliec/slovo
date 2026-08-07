package cx.viz.slovo.domain

/** Session-only lesson lives; no persistence, no regen — reset fresh each lesson. */
object Hearts {
    const val MAX = 3
    fun afterMiss(current: Int): Int = (current - 1).coerceAtLeast(0)
    fun isDepleted(current: Int): Boolean = current <= 0
}
