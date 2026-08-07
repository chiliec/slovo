package cx.viz.slovo.data

import cx.viz.slovo.db.SlovoDatabase
import cx.viz.slovo.domain.CardProgress
import cx.viz.slovo.domain.MasteryCalculator
import cx.viz.slovo.domain.ProfileStatsCalculator
import cx.viz.slovo.domain.ProfileSummary
import cx.viz.slovo.domain.SrsScheduler
import cx.viz.slovo.domain.SrsSnapshot
import cx.viz.slovo.domain.StreakCalculator
import cx.viz.slovo.domain.UserProfile
import cx.viz.slovo.domain.UserStats
import cx.viz.slovo.domain.XpCalculator

class ProgressRepository(private val db: SlovoDatabase) {
    private val cards = db.cardQueries
    private val lessons = db.lessonQueries
    private val statsQ = db.statsQueries
    private val profileQ = db.userProfileQueries

    init { statsQ.initRow(); profileQ.initRow() }

    fun recordAnswer(cardId: String, correct: Boolean, todayEpochDay: Long) {
        cards.insertOrIgnore(cardId = cardId)
        val currentBox = cards.selectBox(cardId).executeAsOne().toInt()
        cards.updateAnswer(
            cardId = cardId,
            correctInc = if (correct) 1L else 0L,
            wrongInc = if (correct) 0L else 1L,
            now = todayEpochDay,
            box = SrsScheduler.nextBox(currentBox, correct).toLong(),
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
                box = row.box.toInt(),
                lastSeenDay = row.last_seen,
            )
        }
    }

    fun percent(cardIds: List<String>): Int =
        MasteryCalculator.percent(cardIds, forCards(cardIds))

    /** Card ids to serve in a review drill: due first, padded with seen-but-not-due. */
    fun pickDrill(cardIds: List<String>, today: Long, size: Int): List<String> {
        val seen = forCards(cardIds).values.filter { it.lastSeenDay != null }
        return SrsScheduler.pickForDrill(seen, today, size)
    }

    /** How many already-seen cards are due for review as of [today]. */
    fun dueCount(cardIds: List<String>, today: Long): Int {
        val seen = forCards(cardIds).values.filter { it.lastSeenDay != null }
        return SrsScheduler.dueCount(seen, today)
    }

    /** Box histogram + 7-day due forecast over seen cards, for the YOU-screen SRS view. */
    fun srsSnapshot(cardIds: List<String>, today: Long): SrsSnapshot {
        val seen = forCards(cardIds).values.filter { it.lastSeenDay != null }.toList()
        return SrsSnapshot(
            boxCounts = SrsScheduler.boxHistogram(seen).toList(),
            dueForecast = SrsScheduler.dueForecast(seen, today).toList(),
            seenCount = seen.size,
        )
    }

    /** Lifetime rollup for the YOU-screen OVERVIEW section. Lesson totals come from content. */
    fun profileSummary(allCardIds: List<String>, lessonsCompleted: Int, lessonsTotal: Int): ProfileSummary =
        ProfileStatsCalculator.summarize(allCardIds, forCards(allCardIds), lessonsCompleted, lessonsTotal)

    fun completedLessonIds(): Set<String> =
        lessons.selectAll().executeAsList().filter { it.completed == 1L }.map { it.lesson_id }.toSet()

    fun stats(): UserStats = statsQ.select().executeAsOne().let {
        UserStats(
            xp = it.xp.toInt(), streakDays = it.streak_days.toInt(),
            lastActiveEpochDay = it.last_active_day, streakFreezes = it.streak_freezes.toInt(),
        )
    }

    /** Forgives one missed day: spends a freeze and rolls last-active forward so the streak survives. */
    fun useStreakFreeze(todayEpochDay: Long): UserStats {
        statsQ.useFreeze(day = todayEpochDay - 1)
        return stats()
    }

    fun resetStreak(todayEpochDay: Long): UserStats {
        statsQ.resetStreak(day = todayEpochDay)
        return stats()
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

    fun userProfile(): UserProfile = profileQ.select().executeAsOne().let {
        UserProfile(
            goal = it.goal, level = it.level, dailyGoalMinutes = it.daily_goal_minutes.toInt(),
            startUnitId = it.start_unit_id, onboarded = it.onboarded == 1L,
        )
    }

    fun completeOnboarding(goal: String, level: String, dailyGoalMinutes: Int, startUnitId: String) {
        profileQ.complete(
            goal = goal, level = level,
            dailyGoalMinutes = dailyGoalMinutes.toLong(), startUnitId = startUnitId,
        )
    }
}
