package cx.viz.slovo.domain

import kotlin.math.roundToInt

/** Read-only rollup of lifetime progress for the YOU-screen OVERVIEW section. */
data class ProfileSummary(
    val accuracyPercent: Int?,   // null when no answers yet → UI renders "—"
    val totalAnswers: Int,
    val cardsSeen: Int,
    val cardsTotal: Int,
    val cardsMastered: Int,
    val lessonsCompleted: Int,
    val lessonsTotal: Int,
)

object ProfileStatsCalculator {
    fun summarize(
        allCardIds: List<String>,
        progress: Map<String, CardProgress>,
        lessonsCompleted: Int,
        lessonsTotal: Int,
    ): ProfileSummary {
        val entries = allCardIds.mapNotNull { progress[it] }
        val totalCorrect = entries.sumOf { it.correct }
        val totalWrong = entries.sumOf { it.wrong }
        val answers = totalCorrect + totalWrong
        return ProfileSummary(
            accuracyPercent = if (answers == 0) null else (totalCorrect * 100.0 / answers).roundToInt(),
            totalAnswers = answers,
            cardsSeen = allCardIds.count { progress[it]?.lastSeenDay != null },
            cardsTotal = allCardIds.size,
            cardsMastered = allCardIds.count { MasteryCalculator.isMastered(progress[it]) },
            lessonsCompleted = lessonsCompleted,
            lessonsTotal = lessonsTotal,
        )
    }
}
