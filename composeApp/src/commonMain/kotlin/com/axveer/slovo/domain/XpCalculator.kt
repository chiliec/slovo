package com.axveer.slovo.domain

object XpCalculator {
    const val XP_PER_CORRECT = 10
    const val LESSON_COMPLETION_BONUS = 20
    fun sessionXp(correctCount: Int): Int = correctCount * XP_PER_CORRECT + LESSON_COMPLETION_BONUS
}
