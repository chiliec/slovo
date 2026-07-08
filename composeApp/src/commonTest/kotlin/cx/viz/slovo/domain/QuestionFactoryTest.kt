package cx.viz.slovo.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionFactoryTest {
    private val cards = listOf(
        Card("1", "привет", "privet", "hello", "1.m4a"),
        Card("2", "спасибо", "spasibo", "thank you", "2.m4a"),
        Card("3", "да", "da", "yes", "3.m4a"),
        Card("4", "нет", "net", "no", "4.m4a"),
        Card("5", "пожалуйста", "pozhaluysta", "please", "5.m4a"),
    )

    @Test fun listening_uses_audio_and_english_options() {
        val q = QuestionFactory(Random(1)).build(cards[0], cards, LessonKind.LISTENING, isMastered = false)
        assertEquals(QuestionMode.LISTEN, q.mode)
        assertEquals("1.m4a", q.audio)
        assertEquals("hello", q.options[q.correctIndex])
        assertTrue(q.options.size in 2..4)
    }

    @Test fun recall_shows_english_prompt_and_russian_options() {
        val q = QuestionFactory(Random(1)).build(cards[0], cards, LessonKind.RECALL, isMastered = false)
        assertEquals(QuestionMode.PRODUCE, q.mode)
        assertEquals(null, q.audio)
        assertEquals("привет", q.options[q.correctIndex])
    }

    @Test fun vocab_defaults_to_read() {
        val q = QuestionFactory(Random(1)).build(cards[0], cards, LessonKind.VOCAB, isMastered = false)
        assertEquals(QuestionMode.READ, q.mode)
        assertEquals("hello", q.options[q.correctIndex])
    }

    @Test fun options_have_no_duplicate_correct_answer() {
        val q = QuestionFactory(Random(3)).build(cards[0], cards, LessonKind.VOCAB, isMastered = false)
        assertEquals(1, q.options.count { it == "hello" })
    }
}
