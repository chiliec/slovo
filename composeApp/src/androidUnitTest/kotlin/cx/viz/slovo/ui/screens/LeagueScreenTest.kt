package cx.viz.slovo.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import cx.viz.slovo.ui.theme.SlovoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LeagueScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsComingSoonStub() {
        rule.setContent { SlovoTheme { LeagueScreen() } }

        rule.onNodeWithText("LEAGUES — COMING SOON").assertIsDisplayed()
        rule.onNodeWithText("Compete with other learners in a future update.").assertIsDisplayed()
    }
}
