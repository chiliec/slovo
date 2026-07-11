package cx.viz.slovo.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
class YouScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun waitForText(text: String) =
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun freshProfileShowsMasteryAndEmptySrsMessage() {
        rule.setContent { SlovoTheme { YouScreen(testModule()) } }
        waitForText("YOU")

        rule.onNodeWithText("TOTAL XP").assertIsDisplayed()
        rule.onNodeWithText("Basics").assertIsDisplayed()
        rule.onNodeWithText("0%").assertIsDisplayed()
        rule.onNodeWithText("Study a lesson to start building your review deck.").assertIsDisplayed()
    }

    @Test
    fun seenCardsRevealSrsCharts() {
        val module = testModule {
            recordAnswer("c1", correct = true, todayEpochDay = currentEpochDay())
            recordAnswer("c2", correct = false, todayEpochDay = currentEpochDay())
        }
        rule.setContent { SlovoTheme { YouScreen(module) } }
        waitForText("YOU")

        rule.onNodeWithText("BOX STRENGTH").assertIsDisplayed()
        rule.onNodeWithText("DUE FORECAST").assertIsDisplayed()
    }
}
