package cx.viz.slovo.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PlacementCalculatorTest {
    @Test fun zero_score_starts_at_unit_one() {
        assertEquals(0, PlacementCalculator.startUnitIndex(0))
    }

    @Test fun one_or_two_correct_starts_at_unit_two() {
        assertEquals(1, PlacementCalculator.startUnitIndex(1))
        assertEquals(1, PlacementCalculator.startUnitIndex(2))
    }

    @Test fun perfect_score_starts_at_unit_three() {
        assertEquals(2, PlacementCalculator.startUnitIndex(3))
    }
}
