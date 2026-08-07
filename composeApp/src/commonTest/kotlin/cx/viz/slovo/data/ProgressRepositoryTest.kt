package cx.viz.slovo.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.slovo.db.SlovoDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressRepositoryTest {
    private lateinit var repo: ProgressRepository

    @BeforeTest fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SlovoDatabase.Schema.create(driver)
        repo = ProgressRepository(SlovoDatabase(driver))
    }

    @Test fun recording_a_correct_answer_masters_the_card() {
        repo.recordAnswer("c1", correct = true, todayEpochDay = 20000)
        assertEquals(100, repo.percent(listOf("c1")))
    }

    @Test fun completing_a_lesson_awards_xp_and_starts_streak() {
        val stats = repo.completeLesson("l1", correctCount = 3, todayEpochDay = 20000)
        assertEquals(50, stats.xp)          // 3*10 + 20
        assertEquals(1, stats.streakDays)
        assertTrue("l1" in repo.completedLessonIds())
    }

    @Test fun second_lesson_next_day_increments_streak_and_accumulates_xp() {
        repo.completeLesson("l1", 3, 20000)
        val stats = repo.completeLesson("l2", 2, 20001)
        assertEquals(90, stats.xp)          // 50 + (2*10 + 20)
        assertEquals(2, stats.streakDays)
    }

    @Test fun finishing_a_drill_awards_xp_without_touching_streak() {
        val stats = repo.recordDrillResult(correctCount = 4)
        assertEquals(40, stats.xp)          // 4*10, no completion bonus
        assertEquals(0, stats.streakDays)   // drills don't advance the streak
    }

    @Test fun drill_xp_accumulates_and_preserves_an_existing_streak() {
        repo.completeLesson("l1", 3, 20000)     // 50 xp, streak 1
        val stats = repo.recordDrillResult(correctCount = 2)
        assertEquals(70, stats.xp)          // 50 + 2*10
        assertEquals(1, stats.streakDays)   // streak untouched
    }

    @Test fun recording_a_correct_answer_promotes_the_box_and_stamps_the_day() {
        repo.recordAnswer("c1", correct = true, todayEpochDay = 20000)
        val p = repo.forCards(listOf("c1"))["c1"]!!
        assertEquals(1, p.box)
        assertEquals(20000L, p.lastSeenDay)
    }

    @Test fun a_wrong_answer_after_progress_resets_the_box_to_zero() {
        repo.recordAnswer("c1", correct = true, todayEpochDay = 20000)   // box 1
        repo.recordAnswer("c1", correct = true, todayEpochDay = 20002)   // box 2
        repo.recordAnswer("c1", correct = false, todayEpochDay = 20006)  // reset
        assertEquals(0, repo.forCards(listOf("c1"))["c1"]!!.box)
    }

    @Test fun pickDrill_returns_only_seen_cards_due_first() {
        repo.recordAnswer("seenDue", correct = true, todayEpochDay = 20000)   // box1, due 20002
        repo.recordAnswer("seenFresh", correct = true, todayEpochDay = 20050) // box1, due 20052
        val picked = repo.pickDrill(listOf("seenDue", "seenFresh", "neverSeen"), today = 20051, size = 10)
        assertEquals(listOf("seenDue", "seenFresh"), picked)  // neverSeen excluded; due first
    }

    @Test fun dueCount_counts_seen_cards_that_have_come_due() {
        repo.recordAnswer("a", correct = true, todayEpochDay = 20000)  // due 20002
        repo.recordAnswer("b", correct = true, todayEpochDay = 20000)  // due 20002
        repo.recordAnswer("c", correct = true, todayEpochDay = 20050)  // due 20052
        assertEquals(2, repo.dueCount(listOf("a", "b", "c", "neverSeen"), today = 20003))
    }

    @Test fun srsSnapshot_counts_only_seen_cards_in_box_histogram() {
        repo.recordAnswer("a", correct = true, todayEpochDay = 20000)   // box 1
        repo.recordAnswer("b", correct = true, todayEpochDay = 20000)   // box 1
        repo.recordAnswer("c", correct = false, todayEpochDay = 20000)  // box 0 (seen)
        val snap = repo.srsSnapshot(listOf("a", "b", "c", "neverSeen"), today = 20000)
        assertEquals(3, snap.seenCount)          // neverSeen excluded
        assertEquals(6, snap.boxCounts.size)
        assertEquals(1, snap.boxCounts[0])       // c
        assertEquals(2, snap.boxCounts[1])       // a, b
    }

    @Test fun srsSnapshot_forecast_shifts_left_as_today_advances() {
        repo.recordAnswer("a", correct = true, todayEpochDay = 20000)  // box 1 interval 2 -> due 20002
        assertEquals(1, repo.srsSnapshot(listOf("a"), today = 20000).dueForecast[2]) // offset 2
        assertEquals(1, repo.srsSnapshot(listOf("a"), today = 20002).dueForecast[0]) // now due
    }

    // Simulates a device that already had the app installed at schema v2 (pre-Kosmo,
    // i.e. build 1: card_progress.box present, but no streak_freezes/user_profile/
    // app_settings). Regression test for the crash where those were added straight to
    // the .sq files without a matching .sqm, so Schema.migrate() never created them and
    // ProgressRepository's init block crashed on every launch. See 2.sqm.
    @Test fun migrating_from_a_pre_kosmo_v2_database_does_not_crash_on_init() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, """
            CREATE TABLE card_progress (
                card_id   TEXT NOT NULL PRIMARY KEY,
                seen      INTEGER NOT NULL DEFAULT 0,
                correct   INTEGER NOT NULL DEFAULT 0,
                wrong     INTEGER NOT NULL DEFAULT 0,
                last_seen INTEGER,
                box       INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent(), 0)
        driver.execute(null, """
            CREATE TABLE lesson_progress (
                lesson_id    TEXT NOT NULL PRIMARY KEY,
                completed    INTEGER NOT NULL DEFAULT 0,
                best_correct INTEGER NOT NULL DEFAULT 0,
                last_seen    INTEGER
            )
        """.trimIndent(), 0)
        driver.execute(null, """
            CREATE TABLE user_stats (
                id              INTEGER NOT NULL PRIMARY KEY,
                xp              INTEGER NOT NULL DEFAULT 0,
                streak_days     INTEGER NOT NULL DEFAULT 0,
                last_active_day INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent(), 0)

        SlovoDatabase.Schema.migrate(driver, oldVersion = 2, newVersion = SlovoDatabase.Schema.version)

        ProgressRepository(SlovoDatabase(driver))
    }

    @Test fun profileSummary_aggregates_answers_seen_and_mastered() {
        repo.recordAnswer("c1", correct = true, todayEpochDay = 20000)
        repo.recordAnswer("c1", correct = true, todayEpochDay = 20001)
        repo.recordAnswer("c2", correct = false, todayEpochDay = 20000)
        val s = repo.profileSummary(listOf("c1", "c2", "c3"), lessonsCompleted = 1, lessonsTotal = 4)
        assertEquals(3, s.totalAnswers)      // c1: 2 correct, c2: 1 wrong
        assertEquals(67, s.accuracyPercent)  // 2/3 = 66.67 -> 67
        assertEquals(2, s.cardsSeen)         // c1, c2 (c3 never answered)
        assertEquals(3, s.cardsTotal)
        assertEquals(1, s.cardsMastered)     // c1 (correct > 0)
        assertEquals(1, s.lessonsCompleted)
        assertEquals(4, s.lessonsTotal)
    }
}
