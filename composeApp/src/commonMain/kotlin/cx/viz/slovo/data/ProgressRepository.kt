package cx.viz.slovo.data

import cx.viz.slovo.db.SlovoDatabase
import cx.viz.slovo.domain.CardProgress
import cx.viz.slovo.domain.MasteryCalculator
import cx.viz.slovo.domain.StreakCalculator
import cx.viz.slovo.domain.UserStats
import cx.viz.slovo.domain.XpCalculator

class ProgressRepository(private val db: SlovoDatabase) {
    private val cards = db.cardQueries
    private val lessons = db.lessonQueries
    private val statsQ = db.statsQueries

    init { statsQ.initRow() }

    fun recordAnswer(cardId: String, correct: Boolean) {
        cards.insertOrIgnore(cardId = cardId)
        cards.updateAnswer(
            cardId = cardId,
            correctInc = if (correct) 1L else 0L,
            wrongInc = if (correct) 0L else 1L,
            now = 0L,  // v1: last_seen unused; SRS scheduling is deferred
        )
    }

    fun forCards(ids: List<String>): Map<String, CardProgress> {
        if (ids.isEmpty()) return emptyMap()
        return cards.selectByIds(ids).executeAsList().associate { row ->
            row.card_id to CardProgress(
                cardId = row.card_id,
                seen = row.seen.toInt(),
                correct = row.correct.toInt(),
                wrong = row.wrong.toInt(),
            )
        }
    }

    fun percent(cardIds: List<String>): Int =
        MasteryCalculator.percent(cardIds, forCards(cardIds))

    fun completedLessonIds(): Set<String> =
        lessons.selectAll().executeAsList().filter { it.completed == 1L }.map { it.lesson_id }.toSet()

    fun stats(): UserStats = statsQ.select().executeAsOne().let {
        UserStats(xp = it.xp.toInt(), streakDays = it.streak_days.toInt(), lastActiveEpochDay = it.last_active_day)
    }

    /** Awards drill XP on top of the current total; leaves streak and last-active day untouched. */
    fun recordDrillResult(correctCount: Int): UserStats {
        val prev = stats()
        val newXp = prev.xp + XpCalculator.drillXp(correctCount)
        statsQ.update(xp = newXp.toLong(), streak = prev.streakDays.toLong(), day = prev.lastActiveEpochDay)
        return stats()
    }

    fun completeLesson(lessonId: String, correctCount: Int, todayEpochDay: Long): UserStats {
        lessons.insertOrIgnore(lessonId = lessonId)
        lessons.markComplete(lessonId = lessonId, correct = correctCount.toLong(), now = todayEpochDay)
        val prev = stats()
        val newStreak = StreakCalculator.next(prev.streakDays, prev.lastActiveEpochDay, todayEpochDay)
        val newXp = prev.xp + XpCalculator.sessionXp(correctCount)
        statsQ.update(xp = newXp.toLong(), streak = newStreak.toLong(), day = todayEpochDay)
        return stats()
    }
}
