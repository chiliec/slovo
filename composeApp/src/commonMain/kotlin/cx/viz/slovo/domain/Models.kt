package cx.viz.slovo.domain

enum class QuestionMode { LISTEN, READ, PRODUCE, TYPE, WORD_BANK, PAIR_MATCH, SPEAK }
enum class LessonKind { VOCAB, LISTENING, RECALL }

data class Card(
    val id: String,
    val russian: String,
    val transliteration: String,
    val english: String,
    val audio: String,
    val note: String? = null,
)

data class Lesson(
    val id: String,
    val title: String,
    val kind: LessonKind,
    val cardIds: List<String>,
)

data class UnitMeta(val id: String, val title: String, val lessonCount: Int)
data class LearnUnit(val meta: UnitMeta, val lessons: List<Lesson>, val cards: List<Card>)

data class Question(
    val mode: QuestionMode,
    val card: Card,
    val promptText: String,
    val audio: String?,
    val options: List<String>,
    val correctIndex: Int,
    /** PAIR_MATCH only: the 3 cards to match RU↔EN; [card] is [pairCards].first() for compatibility. */
    val pairCards: List<Card> = emptyList(),
)

data class CardProgress(
    val cardId: String,
    val seen: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val box: Int = 0,
    val lastSeenDay: Long? = null,
)

/** Read-only snapshot of the review deck for the SRS visualization. */
data class SrsSnapshot(
    val boxCounts: List<Int>,    // size 6, index = box 0..5
    val dueForecast: List<Int>,  // size 7, index 0 = today/overdue … 6 = +6 days
    val seenCount: Int,          // seen cards total; drives the empty state
)

data class LessonProgress(
    val lessonId: String,
    val completed: Boolean = false,
    val bestCorrect: Int = 0,
)

data class UserStats(
    val xp: Int = 0,
    val streakDays: Int = 0,
    val lastActiveEpochDay: Long = 0,
)

data class UserProfile(
    val goal: String = "",
    val level: String = "",
    val dailyGoalMinutes: Int = 15,
    val startUnitId: String = "",
    val onboarded: Boolean = false,
)
