package cx.viz.slovo.domain

/**
 * Leitner-box spaced repetition over whole-day intervals (the clock is day-granular).
 * A card's due day is derived from its last-seen day and box, never stored.
 */
object SrsScheduler {
    /** Review interval in days, indexed by box. */
    val INTERVALS = intArrayOf(1, 2, 4, 7, 14, 30)
    val MAX_BOX = INTERVALS.size - 1

    /** Correct -> promote one box (capped); wrong -> reset to box 0. */
    fun nextBox(box: Int, correct: Boolean): Int =
        if (correct) minOf(box + 1, MAX_BOX) else 0

    /** Day this card next falls due, or null if never seen. */
    fun dueDay(p: CardProgress): Long? =
        p.lastSeenDay?.let { it + INTERVALS[p.box.coerceIn(0, MAX_BOX)] }

    /** A never-seen card is due; a seen card is due on or after its due day. */
    fun isDue(p: CardProgress, today: Long): Boolean {
        val due = dueDay(p) ?: return true
        return today >= due
    }

    fun dueCount(seen: List<CardProgress>, today: Long): Int =
        seen.count { isDue(it, today) }

    /**
     * Card ids for a review drill: due cards first (most overdue first),
     * then seen-but-not-due cards (soonest due first), capped at [size].
     */
    fun pickForDrill(seen: List<CardProgress>, today: Long, size: Int = 10): List<String> {
        val (due, notDue) = seen.partition { isDue(it, today) }
        val dueSorted = due.sortedBy { dueDay(it) ?: Long.MIN_VALUE }
        val notDueSorted = notDue.sortedBy { dueDay(it) ?: Long.MAX_VALUE }
        return (dueSorted + notDueSorted).take(size).map { it.cardId }
    }
}
