package cx.viz.slovo.domain

import kotlin.math.roundToInt

object MasteryCalculator {
    fun isMastered(p: CardProgress?): Boolean = (p?.correct ?: 0) > 0

    fun percent(cardIds: List<String>, progress: Map<String, CardProgress>): Int {
        if (cardIds.isEmpty()) return 0
        val mastered = cardIds.count { isMastered(progress[it]) }
        return (mastered.toDouble() / cardIds.size * 100).roundToInt()
    }
}
