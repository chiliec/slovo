package cx.viz.slovo.domain

enum class QuestionMode { LISTEN, READ, PRODUCE }
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
)

data class CardProgress(
    val cardId: String,
    val seen: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
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
