package cx.viz.slovo.domain

/** Placement-quiz score (0..3 correct) → which unit index to start from. */
object PlacementCalculator {
    fun startUnitIndex(score: Int): Int = when {
        score <= 0 -> 0
        score <= 2 -> 1
        else -> 2
    }
}
