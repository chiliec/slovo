package cx.viz.slovo.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cx.viz.slovo.ui.testModule
import cx.viz.slovo.ui.theme.SlovoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * HomeScreen contains MishaTicker, an infinite animation that would block Compose's
 * waitForIdle (and thus assertions/clicks) forever. The recipe: let content load
 * with the clock live — waitUntil polls without needing idle — then freeze the clock
 * so subsequent idle-based operations settle instead of chasing the animation.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun waitForTextThenFreeze(text: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        rule.mainClock.autoAdvance = false
    }

    @Test(timeout = 30_000)
    fun showsUpNextAndLocksLaterLesson() {
        rule.setContent {
            SlovoTheme { HomeScreen(testModule(), onOpenLesson = { _, _ -> }, onOpenDrill = {}) }
        }
        waitForTextThenFreeze("SLOVO")

        rule.onNodeWithText("UP NEXT · BASICS").assertIsDisplayed()
        rule.onNodeWithText("START →").assertIsDisplayed()
        // The second lesson is gated on completing the first.
        rule.onNodeWithText("Basics Two").assertIsDisplayed()
        rule.onNodeWithText("LOCKED").assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun startOpensTheFirstIncompleteLesson() {
        var opened: Pair<String, String>? = null
        rule.setContent {
            SlovoTheme {
                HomeScreen(
                    testModule(),
                    onOpenLesson = { u, l -> opened = u to l },
                    onOpenDrill = {},
                )
            }
        }
        waitForTextThenFreeze("START →")

        rule.onNodeWithText("START →").performClick()
        assert(opened == ("u1" to "l1")) { "expected START to open unit u1 / lesson l1, got $opened" }
    }
}
