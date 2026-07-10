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
}
