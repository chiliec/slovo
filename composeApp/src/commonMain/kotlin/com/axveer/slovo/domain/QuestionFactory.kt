package com.axveer.slovo.domain

import kotlin.random.Random

class QuestionFactory(private val rng: Random = Random.Default) {

    fun build(card: Card, pool: List<Card>, kind: LessonKind, isMastered: Boolean): Question {
        val mode = when (kind) {
            LessonKind.LISTENING -> QuestionMode.LISTEN
            LessonKind.RECALL -> QuestionMode.PRODUCE
            LessonKind.VOCAB ->
                if (isMastered && rng.nextInt(5) == 0) QuestionMode.PRODUCE else QuestionMode.READ
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
        val options = (distractors.take(3) + correct).toMutableList()
        shuffle(options)

        val prompt = when (mode) {
            QuestionMode.LISTEN -> "What does this mean?"
            QuestionMode.READ -> "What does \"${card.russian}\" mean?"
            QuestionMode.PRODUCE -> "How do you say \"${card.english}\"?"
        }
        return Question(
            mode = mode,
            card = card,
            promptText = prompt,
            audio = if (mode == QuestionMode.LISTEN) card.audio else null,
            options = options,
            correctIndex = options.indexOf(correct),
        )
    }

    private fun <T> shuffle(list: MutableList<T>) {
        for (i in list.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        }
    }
}
