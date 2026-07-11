package cx.viz.slovo.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import cx.viz.slovo.ui.AppModule
import cx.viz.slovo.ui.theme.SlovoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end Compose UI test for the LessonScreen study→quiz→result flow, run on
 * the JVM via Robolectric. Content comes from a fake repository; progress uses a
 * real in-memory SQLite database; audio is a no-op.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LessonScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val cards = listOf(
        Card("c1", russian = "привет", transliteration = "privet", english = "hello", audio = "a1.m4a"),
        Card("c2", russian = "пока", transliteration = "poka", english = "bye", audio = "a2.m4a"),
        Card("c3", russian = "да", transliteration = "da", english = "yes", audio = "a3.m4a"),
    )
    private val unit = LearnUnit(
        meta = UnitMeta("u1", "Basics", lessonCount = 1),
        lessons = listOf(Lesson("l1", "Greetings", LessonKind.VOCAB, cards.map { it.id })),
        cards = cards,
    )

    private class FakeContent(private val unit: LearnUnit) : ContentRepository {
        override suspend fun units() = listOf(unit.meta)
        override suspend fun unit(unitId: String) = unit
        override suspend fun allCards() = unit.cards
    }

    private fun module(): AppModule {
        // Register the SQLite driver within Robolectric's sandbox classloader:
        // DriverManager only hands out drivers loadable by the caller's loader, so
        // the system-property registration (build.gradle.kts) isn't visible here.
        Class.forName("org.sqlite.JDBC")
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SlovoDatabase.Schema.create(driver)
        return AppModule(
            content = FakeContent(unit),
            progress = ProgressRepository(SlovoDatabase(driver)),
            audio = NoopAudioPlayer(),
        )
    }

    private fun setContent(onDone: () -> Unit = {}) {
        rule.setContent { SlovoTheme { LessonScreen(module(), "u1", "l1", onDone) } }
    }

    private fun waitForText(text: String) =
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun studyViewShowsFirstCardWithTransliteration() {
        setContent()
        waitForText("1 / 3")

        rule.onNodeWithText("привет").assertIsDisplayed()
        rule.onNodeWithText("privet").assertIsDisplayed()
        rule.onNodeWithText("hello").assertIsDisplayed()
        rule.onNodeWithText("🔊 PLAY").assertIsDisplayed()
        rule.onNodeWithText("NEXT →").assertIsDisplayed()
    }

    @Test
    fun translitToggleHidesTransliteration() {
        setContent()
        waitForText("1 / 3")

        rule.onNodeWithText("privet").assertIsDisplayed()
        rule.onNodeWithText("translit: on").performClick()

        rule.onNodeWithText("privet").assertDoesNotExist()
        rule.onNodeWithText("translit: off").assertIsDisplayed()
    }

    @Test
    fun lastStudyCardOffersPractice() {
        setContent()
        waitForText("1 / 3")

        rule.onNodeWithText("NEXT →").performClick()
        waitForText("2 / 3")
        rule.onNodeWithText("NEXT →").performClick()
        waitForText("3 / 3")

        rule.onNodeWithText("PRACTICE").assertIsDisplayed()
    }

    @Test
    fun answeringAllQuestionsCorrectlyReachesResult() {
        setContent()
        waitForText("1 / 3")

        // Advance through the study cards to the practice entry point.
        rule.onNodeWithText("NEXT →").performClick()
        rule.onNodeWithText("NEXT →").performClick()
        rule.onNodeWithText("PRACTICE").performClick()

        // VOCAB + fresh cards → READ questions; the correct option is each card's
        // English, and questions are built in card order.
        for (english in cards.map { it.english }) {
            rule.onNodeWithText(english).performClick()
            rule.onNodeWithText("CONTINUE →").performClick()
        }

        waitForText("LESSON DONE")
        rule.onNodeWithText("3 / 3 correct").assertIsDisplayed()
    }
}
