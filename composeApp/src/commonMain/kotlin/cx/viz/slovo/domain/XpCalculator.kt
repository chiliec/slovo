package cx.viz.slovo.domain

object XpCalculator {
    const val XP_PER_CORRECT = 10
    const val LESSON_COMPLETION_BONUS = 20
    fun sessionXp(correctCount: Int): Int = correctCount * XP_PER_CORRECT + LESSON_COMPLETION_BONUS

    /** Drills are extra practice: reward each correct answer, no lesson-completion bonus. */
    fun drillXp(correctCount: Int): Int = correctCount * XP_PER_CORRECT
}
