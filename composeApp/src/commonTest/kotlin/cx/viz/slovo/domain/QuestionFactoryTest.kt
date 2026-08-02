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

    @Test fun recall_is_typed_with_no_options() {
        val q = QuestionFactory(Random(1)).build(cards[0], cards, LessonKind.RECALL, isMastered = false)
        assertEquals(QuestionMode.TYPE, q.mode)
        assertTrue(q.options.isEmpty())
        assertEquals("Type the English meaning", q.promptText)
        assertEquals("hello", q.card.english)
    }

    @Test fun drill_prefers_typing_for_mastered_cards() {
        val q = QuestionFactory(Random(1))
            .build(cards[0], cards, LessonKind.VOCAB, isMastered = true, preferTyping = true)
        assertEquals(QuestionMode.TYPE, q.mode)
        assertTrue(q.options.isEmpty())
    }

    @Test fun drill_keeps_choices_for_unmastered_cards() {
        val q = QuestionFactory(Random(1))
            .build(cards[0], cards, LessonKind.VOCAB, isMastered = false, preferTyping = true)
        assertTrue(q.mode != QuestionMode.TYPE)
        assertTrue(q.options.isNotEmpty())
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

    /**
     * Invariants that must hold for every option-based question regardless of RNG:
     * correctIndex points at the correct answer, it appears exactly once, all
     * options are distinct, and the count stays within the 1..4 UI budget.
     */
    @Test fun option_invariants_hold_across_many_seeds() {
        for (seed in 0 until 300) {
            val factory = QuestionFactory(Random(seed))
            val card = cards[seed % cards.size]
            for (kind in listOf(LessonKind.LISTENING, LessonKind.VOCAB)) {
                val q = factory.build(card, cards, kind, isMastered = false)
                if (q.mode == QuestionMode.TYPE) continue
                val correct = q.options[q.correctIndex]
                assertEquals(card.english, correct, "seed=$seed kind=$kind correctIndex mismatch")
                assertEquals(1, q.options.count { it == correct }, "seed=$seed duplicate correct answer")
                assertEquals(q.options.size, q.options.toSet().size, "seed=$seed duplicate option")
                assertTrue(q.options.size in 1..4, "seed=$seed option count ${q.options.size} out of range")
            }
        }
    }

    @Test fun single_card_pool_yields_lone_correct_option() {
        val q = QuestionFactory(Random(1)).build(cards[0], listOf(cards[0]), LessonKind.LISTENING, isMastered = false)
        assertEquals(listOf("hello"), q.options)
        assertEquals(0, q.correctIndex)
    }

    @Test fun small_pool_caps_options_at_available_distractors() {
        val pool = cards.take(3) // 1 correct + 2 possible distractors
        val q = QuestionFactory(Random(1)).build(cards[0], pool, LessonKind.LISTENING, isMastered = false)
        assertEquals(3, q.options.size)
        assertEquals("hello", q.options[q.correctIndex])
    }

    @Test fun produce_mode_uses_russian_options() {
        // PRODUCE requires VOCAB + mastered + rng.nextInt(5) == 0; find a seed that lands there.
        var checked = false
        for (seed in 0 until 100) {
            val q = QuestionFactory(Random(seed)).build(cards[0], cards, LessonKind.VOCAB, isMastered = true)
            if (q.mode != QuestionMode.PRODUCE) continue
            checked = true
            assertEquals("привет", q.options[q.correctIndex])
            assertTrue(q.options.all { opt -> cards.any { it.russian == opt } }, "seed=$seed non-russian option")
            assertEquals("How do you say \"hello\"?", q.promptText)
            break
        }
        assertTrue(checked, "no seed in 0..99 produced a PRODUCE question")
    }
}
