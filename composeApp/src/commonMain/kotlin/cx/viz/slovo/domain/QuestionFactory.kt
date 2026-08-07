package cx.viz.slovo.domain

import kotlin.random.Random

class QuestionFactory(private val rng: Random = Random.Default) {

    fun build(
        card: Card,
        pool: List<Card>,
        kind: LessonKind,
        isMastered: Boolean,
        preferTyping: Boolean = false,
    ): Question {
        val mode = when {
            kind == LessonKind.RECALL -> QuestionMode.TYPE
            preferTyping && isMastered -> QuestionMode.TYPE
            kind == LessonKind.LISTENING -> QuestionMode.LISTEN
            kind == LessonKind.VOCAB && isMastered && rng.nextInt(5) == 0 -> QuestionMode.PRODUCE
            else -> QuestionMode.READ
        }

        if (mode == QuestionMode.TYPE) {
            return Question(
                mode = mode,
                card = card,
                promptText = "Type the English meaning",
                audio = card.audio,
                options = emptyList(),
                correctIndex = 0,
            )
        }

        val answerOf: (Card) -> String =
            if (mode == QuestionMode.PRODUCE) { c -> c.russian } else { c -> c.english }

        val correct = answerOf(card)
        val distractors = pool.asSequence()
            .filter { it.id != card.id }
            .map(answerOf)
            .filter { it != correct }
            .distinct()
            .toMutableList()
        shuffle(distractors)
        // Insert the correct answer at a random position and track that index
        // directly, rather than indexOf() which returns the wrong slot when a
        // distractor happens to share the correct answer's text.
        val options = distractors.take(3).toMutableList()
        val correctIndex = if (options.isEmpty()) 0 else rng.nextInt(options.size + 1)
        options.add(correctIndex, correct)

        val prompt = when (mode) {
            QuestionMode.LISTEN -> "What does this mean?"
            QuestionMode.READ -> "What does \"${card.russian}\" mean?"
            QuestionMode.PRODUCE -> "How do you say \"${card.english}\"?"
            else -> ""   // TYPE handled above; WORD_BANK/PAIR_MATCH/SPEAK built elsewhere
        }
        return Question(
            mode = mode,
            card = card,
            promptText = prompt,
            audio = if (mode == QuestionMode.LISTEN) card.audio else null,
            options = options,
            correctIndex = correctIndex,
        )
    }

    /** Chip bank = target English words + distractor words from other cards, shuffled. */
    fun buildWordBank(card: Card, pool: List<Card>): Question {
        val target = card.english.split(" ").filter { it.isNotBlank() }
        val distractors = pool.asSequence()
            .filter { it.id != card.id }
            .flatMap { it.english.split(" ") }
            .filter { it.isNotBlank() && it !in target }
            .distinct()
            .toMutableList()
        shuffle(distractors)
        val chips = (target + distractors.take((4 - target.size).coerceAtLeast(0))).toMutableList()
        shuffle(chips)
        return Question(QuestionMode.WORD_BANK, card, "What does \"${card.russian}\" mean?", card.audio, chips, 0)
    }

    /** Picks 3 cards from [pool] (or all of it, if smaller) to match RU↔EN. */
    fun buildPairMatch(pool: List<Card>): Question {
        val chosen = pool.toMutableList().also { shuffle(it) }.take(minOf(3, pool.size))
        return Question(QuestionMode.PAIR_MATCH, chosen.first(), "Match the pairs", null, emptyList(), 0, pairCards = chosen)
    }

    fun buildSpeak(card: Card): Question =
        Question(QuestionMode.SPEAK, card, "SAY IT OUT LOUD", card.audio, emptyList(), 0)

    private fun <T> shuffle(list: MutableList<T>) {
        for (i in list.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        }
    }
}
