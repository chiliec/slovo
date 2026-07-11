package cx.viz.slovo.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cx.viz.slovo.ui.theme.SlovoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Compose UI tests for the reusable MISHA design-system components, run on the
 * JVM via Robolectric. These are dependency-free — pure composables over inputs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MishaComponentsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun mishaButtonRendersLabelAndFiresClick() {
        var clicks = 0
        rule.setContent { SlovoTheme { MishaButton("TAP ME") { clicks++ } } }

        rule.onNodeWithText("TAP ME").assertIsDisplayed()
        rule.onNodeWithText("TAP ME").performClick()

        assert(clicks == 1) { "expected exactly one click, got $clicks" }
    }

    @Test
    fun mishaStatChipShowsValueAndLabel() {
        rule.setContent {
            SlovoTheme {
                MishaStatChip(
                    value = "42",
                    label = "XP",
                    background = cx.viz.slovo.ui.theme.Slovo.Yellow,
                    textColor = cx.viz.slovo.ui.theme.Slovo.Ink,
                )
            }
        }

        rule.onNodeWithText("42").assertIsDisplayed()
        rule.onNodeWithText("XP").assertIsDisplayed()
    }

    @Test
    fun lessonRowCurrentShowsGoAndIsClickable() {
        var clicked = false
        rule.setContent {
            SlovoTheme {
                LessonRow(
                    index = 3, title = "Greetings", subtitle = "5 cards",
                    done = false, current = true, locked = false, onClick = { clicked = true },
                )
            }
        }

        rule.onNodeWithText("03").assertIsDisplayed()
        rule.onNodeWithText("Greetings").assertIsDisplayed()
        rule.onNodeWithText("5 cards").assertIsDisplayed()
        rule.onNodeWithText("GO →").assertIsDisplayed()

        rule.onNodeWithText("Greetings").performClick()
        assert(clicked) { "expected an unlocked row to be clickable" }
    }

    @Test
    fun lessonRowDoneShowsDoneBadge() {
        rule.setContent {
            SlovoTheme {
                LessonRow(
                    index = 1, title = "Numbers", subtitle = "8 cards",
                    done = true, current = false, locked = false, onClick = {},
                )
            }
        }

        rule.onNodeWithText("DONE").assertIsDisplayed()
    }

    @Test
    fun lessonRowLockedHidesSubtitleAndSwallowsClicks() {
        var clicked = false
        rule.setContent {
            SlovoTheme {
                LessonRow(
                    index = 9, title = "Advanced", subtitle = "12 cards",
                    done = false, current = false, locked = true, onClick = { clicked = true },
                )
            }
        }

        rule.onNodeWithText("LOCKED").assertIsDisplayed()
        rule.onNodeWithText("🔒").assertIsDisplayed()

        // A locked row has no click handler, so tapping it must not navigate.
        rule.onNodeWithText("Advanced").performClick()
        assert(!clicked) { "a locked row must not be clickable" }
    }
}
