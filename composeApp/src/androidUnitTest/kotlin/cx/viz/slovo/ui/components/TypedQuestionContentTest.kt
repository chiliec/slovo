package cx.viz.slovo.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import cx.viz.slovo.domain.Card
import cx.viz.slovo.domain.Question
import cx.viz.slovo.domain.QuestionMode
import cx.viz.slovo.ui.theme.SlovoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Compose UI tests for [TypedQuestionContent], run on the JVM via Robolectric —
 * no device or emulator required. Exercises the typed-recall grading flow end to end.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TypedQuestionContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val question = Question(
        mode = QuestionMode.TYPE,
        card = Card(
            id = "c1",
            russian = "спасибо",
            transliteration = "spasibo",
            english = "thank you",
            audio = "spasibo.m4a",
        ),
        promptText = "Type the English",
        audio = null,
        options = emptyList(),
        correctIndex = 0,
    )

    private fun setContent(onContinue: (Boolean) -> Unit = {}) {
        rule.setContent {
            SlovoTheme {
                TypedQuestionContent(
                    question = question,
                    header = "TYPED RECALL",
                    onPlay = {},
                    onContinue = onContinue,
                )
            }
        }
    }

    @Test
    fun rendersHeaderPromptAndRussian() {
        setContent()

        rule.onNodeWithText("TYPED RECALL").assertIsDisplayed()
        rule.onNodeWithText("Type the English").assertIsDisplayed()
        rule.onNodeWithText("спасибо").assertIsDisplayed()
        rule.onNodeWithText("SUBMIT").assertIsDisplayed()
    }

    @Test
    fun exactAnswerGradesCorrect() {
        setContent()

        rule.onNode(hasSetTextAction()).performTextInput("thank you")
        rule.onNodeWithText("SUBMIT").performClick()

        rule.onNodeWithText("Correct!").assertIsDisplayed()
        rule.onNodeWithText("CONTINUE →").assertIsDisplayed()
    }

    @Test
    fun typoAnswerGradesAlmostAndRevealsCanonical() {
        setContent()

        rule.onNode(hasSetTextAction()).performTextInput("thnk you")
        rule.onNodeWithText("SUBMIT").performClick()

        rule.onNodeWithText("Almost — it's: thank you").assertIsDisplayed()
    }

    @Test
    fun wrongAnswerRevealsCanonicalAndReportsIncorrect() {
        var continued: Boolean? = null
        setContent(onContinue = { continued = it })

        rule.onNode(hasSetTextAction()).performTextInput("goodbye")
        rule.onNodeWithText("SUBMIT").performClick()

        rule.onNodeWithText("Answer: thank you").assertIsDisplayed()
        rule.onNodeWithText("CONTINUE →").performClick()

        assert(continued == false) { "expected onContinue(false) for a wrong answer, got $continued" }
    }
}
