package cx.viz.slovo.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AnswerCheckerTest {
    private fun verdict(typed: String, expected: String) =
        AnswerChecker.check(typed, expected).verdict

    @Test fun exact_match_is_correct() {
        assertEquals(AnswerChecker.Verdict.CORRECT, verdict("hello", "hello"))
    }

    @Test fun case_and_whitespace_insensitive() {
        assertEquals(AnswerChecker.Verdict.CORRECT, verdict("  HeLLo  ", "hello"))
    }

    @Test fun punctuation_insensitive() {
        assertEquals(AnswerChecker.Verdict.CORRECT, verdict("good morning", "Good morning!"))
    }

    @Test fun apostrophes_are_ignored() {
        assertEquals(AnswerChecker.Verdict.CORRECT, verdict("dont", "don't"))
        assertEquals(AnswerChecker.Verdict.CORRECT, verdict("I dont understand", "I don't understand."))
    }

    @Test fun slash_alternatives_each_accepted() {
        assertEquals(AnswerChecker.Verdict.CORRECT, verdict("please", "You're welcome. / Please."))
        assertEquals(AnswerChecker.Verdict.CORRECT, verdict("youre welcome", "You're welcome. / Please."))
    }

    @Test fun near_miss_within_threshold_is_almost() {
        assertEquals(AnswerChecker.Verdict.ALMOST, verdict("helo", "hello"))      // len 5 -> 1 edit
        assertEquals(AnswerChecker.Verdict.ALMOST, verdict("pleese", "please"))   // len 6 -> 1 edit
    }

    @Test fun over_threshold_is_wrong() {
        assertEquals(AnswerChecker.Verdict.WRONG, verdict("halo", "hello"))       // 2 edits, len 5 -> max 1
        assertEquals(AnswerChecker.Verdict.WRONG, verdict("thanks", "thank you"))
    }

    @Test fun short_answers_require_exact_spelling() {
        assertEquals(AnswerChecker.Verdict.WRONG, verdict("ye", "yes"))           // len 3 -> 0 edits
    }

    @Test fun long_phrase_tolerates_two_edits() {
        assertEquals(AnswerChecker.Verdict.ALMOST, verdict("thankyou", "thank you")) // len 9 -> up to 2 edits
    }

    @Test fun empty_input_is_wrong() {
        assertEquals(AnswerChecker.Verdict.WRONG, verdict("", "hello"))
        assertEquals(AnswerChecker.Verdict.WRONG, verdict("   ", "hello"))
    }

    @Test fun canonical_is_expected_verbatim() {
        assertEquals("Good morning!", AnswerChecker.check("good morning", "Good morning!").canonical)
    }
}
