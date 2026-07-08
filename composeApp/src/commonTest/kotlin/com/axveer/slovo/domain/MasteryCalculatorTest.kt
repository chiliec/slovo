package com.axveer.slovo.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MasteryCalculatorTest {
    @Test fun mastered_when_correct_at_least_once() {
        assertTrue(MasteryCalculator.isMastered(CardProgress("a", correct = 1)))
        assertFalse(MasteryCalculator.isMastered(CardProgress("a", correct = 0)))
        assertFalse(MasteryCalculator.isMastered(null))
    }

    @Test fun percent_rounds_mastered_ratio() {
        val progress = mapOf(
            "a" to CardProgress("a", correct = 1),
            "b" to CardProgress("b", correct = 0),
        )
        assertEquals(50, MasteryCalculator.percent(listOf("a", "b"), progress))
        assertEquals(0, MasteryCalculator.percent(emptyList(), progress))
        assertEquals(100, MasteryCalculator.percent(listOf("a"), progress))
    }
}
