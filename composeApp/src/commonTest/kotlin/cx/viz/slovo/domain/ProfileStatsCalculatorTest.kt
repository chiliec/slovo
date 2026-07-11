package cx.viz.slovo.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileStatsCalculatorTest {

    private fun p(id: String, correct: Int, wrong: Int, lastSeenDay: Long? = 1L) =
        id to CardProgress(cardId = id, correct = correct, wrong = wrong, lastSeenDay = lastSeenDay)

    @Test fun summarizes_accuracy_seen_mastered_and_answers() {
        val progress = mapOf(
            p("c1", correct = 2, wrong = 0),   // seen, mastered
            p("c2", correct = 1, wrong = 3),   // seen, mastered
            p("c3", correct = 0, wrong = 1),   // seen, not mastered
        )
        val s = ProfileStatsCalculator.summarize(
            allCardIds = listOf("c1", "c2", "c3", "c4"),
            progress = progress,
            lessonsCompleted = 2,
            lessonsTotal = 5,
        )
        assertEquals(43, s.accuracyPercent)   // 3 correct / 7 answers = 42.857 -> 43
        assertEquals(7, s.totalAnswers)
        assertEquals(3, s.cardsSeen)          // c1, c2, c3 (c4 absent)
        assertEquals(4, s.cardsTotal)
        assertEquals(2, s.cardsMastered)      // c1, c2 (correct > 0)
        assertEquals(2, s.lessonsCompleted)
        assertEquals(5, s.lessonsTotal)
    }

    @Test fun accuracy_is_null_with_no_answers() {
        val s = ProfileStatsCalculator.summarize(
            allCardIds = listOf("c1", "c2"),
            progress = emptyMap(),
            lessonsCompleted = 0,
            lessonsTotal = 3,
        )
        assertNull(s.accuracyPercent)
        assertEquals(0, s.totalAnswers)
        assertEquals(0, s.cardsSeen)
        assertEquals(0, s.cardsMastered)
        assertEquals(2, s.cardsTotal)
    }

    @Test fun a_card_seen_but_never_answered_counts_as_seen_not_mastered() {
        val progress = mapOf(
            "c1" to CardProgress(cardId = "c1", correct = 0, wrong = 0, lastSeenDay = 5L),
        )
        val s = ProfileStatsCalculator.summarize(listOf("c1"), progress, 0, 1)
        assertEquals(1, s.cardsSeen)        // lastSeenDay != null
        assertEquals(0, s.cardsMastered)    // correct == 0
        assertNull(s.accuracyPercent)       // no answers
    }
}
