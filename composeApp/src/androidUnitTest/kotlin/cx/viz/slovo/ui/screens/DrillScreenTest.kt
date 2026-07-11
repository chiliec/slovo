package cx.viz.slovo.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cx.viz.slovo.platform.currentEpochDay
import cx.viz.slovo.ui.testModule
import cx.viz.slovo.ui.theme.SlovoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DrillScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun waitForText(text: String) =
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun emptyDeckOffersGoToLearn() {
        var openedLearn = false
        rule.setContent { SlovoTheme { DrillScreen(testModule(), onOpenLearn = { openedLearn = true }) } }
        waitForText("NOTHING TO REVIEW")

        rule.onNodeWithText("GO TO LEARN →").performClick()
        assert(openedLearn) { "expected the empty-deck CTA to navigate to Learn" }
    }

    @Test
    fun oneSeenCardDrillRunsToResult() {
        // Seed one wrong answer so the card is seen but not "mastered" (isMastered is
        // correct > 0); a drill's preferTyping only produces TYPE for mastered cards, so
        // this yields a one-question READ drill whose correct option is "hello".
        val module = testModule { recordAnswer("c1", correct = false, todayEpochDay = currentEpochDay()) }
        rule.setContent { SlovoTheme { DrillScreen(module, onOpenLearn = {}) } }

        waitForText("hello")
        rule.onNodeWithText("hello").performClick()
        rule.onNodeWithText("FINISH →").performClick()

        waitForText("DRILL DONE")
        rule.onNodeWithText("1 / 1 correct").assertIsDisplayed()
    }
}
