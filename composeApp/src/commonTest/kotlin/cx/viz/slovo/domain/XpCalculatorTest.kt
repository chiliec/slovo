package cx.viz.slovo.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class XpCalculatorTest {
    @Test fun session_xp_is_per_correct_plus_bonus() {
        assertEquals(20, XpCalculator.sessionXp(0))
        assertEquals(50, XpCalculator.sessionXp(3))
    }
}
