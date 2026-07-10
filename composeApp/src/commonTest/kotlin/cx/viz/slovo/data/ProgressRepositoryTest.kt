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
}
