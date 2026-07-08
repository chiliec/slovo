package cx.viz.slovo.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class StreakCalculatorTest {
    @Test fun first_ever_activity_starts_at_one() {
        assertEquals(1, StreakCalculator.next(previousStreak = 0, lastActiveEpochDay = 0, todayEpochDay = 20000))
    }
    @Test fun same_day_does_not_change() {
        assertEquals(5, StreakCalculator.next(5, 20000, 20000))
    }
    @Test fun consecutive_day_increments() {
        assertEquals(6, StreakCalculator.next(5, 20000, 20001))
    }
    @Test fun gap_resets_to_one() {
        assertEquals(1, StreakCalculator.next(5, 20000, 20003))
    }
}
