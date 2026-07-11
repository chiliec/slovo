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
            QuestionMode.TYPE -> ""   // unreachable; handled above
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

    private fun <T> shuffle(list: MutableList<T>) {
        for (i in list.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        }
    }
}
