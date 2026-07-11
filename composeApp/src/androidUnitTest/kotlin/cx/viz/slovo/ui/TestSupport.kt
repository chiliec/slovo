package cx.viz.slovo.ui

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cx.viz.slovo.data.ContentRepository
import cx.viz.slovo.data.ProgressRepository
import cx.viz.slovo.db.SlovoDatabase
import cx.viz.slovo.domain.Card
import cx.viz.slovo.domain.LearnUnit
import cx.viz.slovo.domain.Lesson
import cx.viz.slovo.domain.LessonKind
import cx.viz.slovo.domain.UnitMeta
import cx.viz.slovo.platform.NoopAudioPlayer

/** In-memory content backed by a single fixed unit, for driving screens in tests. */
class FakeContent(private val unit: LearnUnit) : ContentRepository {
    override suspend fun units() = listOf(unit.meta)
    override suspend fun unit(unitId: String) = unit
    override suspend fun allCards() = unit.cards
}

/** The shared fixture: one unit "Basics" with four cards across two VOCAB lessons. */
val sampleCards = listOf(
    Card("c1", russian = "привет", transliteration = "privet", english = "hello", audio = "a1.m4a"),
    Card("c2", russian = "пока", transliteration = "poka", english = "bye", audio = "a2.m4a"),
    Card("c3", russian = "да", transliteration = "da", english = "yes", audio = "a3.m4a"),
    Card("c4", russian = "нет", transliteration = "net", english = "no", audio = "a4.m4a"),
)

val sampleUnit = LearnUnit(
    meta = UnitMeta("u1", "Basics", lessonCount = 2),
    lessons = listOf(
        Lesson("l1", "Greetings", LessonKind.VOCAB, listOf("c1", "c2")),
        Lesson("l2", "Basics Two", LessonKind.VOCAB, listOf("c3", "c4")),
    ),
    cards = sampleCards,
)

/**
 * Build an [AppModule] with fake content, a real in-memory SQLite progress store,
 * and no-op audio. [seed] can pre-populate progress before the screen loads.
 *
 * Registering the SQLite driver here covers Robolectric's sandbox classloader,
 * whose DriverManager can't see the system-property registration.
 */
fun testModule(
    unit: LearnUnit = sampleUnit,
    seed: ProgressRepository.() -> Unit = {},
): AppModule {
    Class.forName("org.sqlite.JDBC")
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    SlovoDatabase.Schema.create(driver)
    val progress = ProgressRepository(SlovoDatabase(driver))
    progress.seed()
    return AppModule(content = FakeContent(unit), progress = progress, audio = NoopAudioPlayer())
}
