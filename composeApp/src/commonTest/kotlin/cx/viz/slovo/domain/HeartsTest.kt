package cx.viz.slovo.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeartsTest {
    @Test fun starts_full() {
        assertEquals(3, Hearts.MAX)
    }
    @Test fun miss_decrements() {
        assertEquals(2, Hearts.afterMiss(3))
    }
    @Test fun miss_never_goes_below_zero() {
        assertEquals(0, Hearts.afterMiss(0))
    }
    @Test fun depleted_at_zero() {
        assertTrue(Hearts.isDepleted(0))
        assertFalse(Hearts.isDepleted(1))
    }
}
