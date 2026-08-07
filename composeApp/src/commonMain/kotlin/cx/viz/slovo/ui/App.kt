package cx.viz.slovo.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import cx.viz.slovo.ui.nav.Dest
import cx.viz.slovo.ui.nav.bottomTabs
import cx.viz.slovo.ui.screens.*
import cx.viz.slovo.ui.theme.Slovo
import cx.viz.slovo.ui.theme.SlovoTheme

@Composable
fun App(module: AppModule) = SlovoTheme {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    Scaffold(
        containerColor = Slovo.Sand,
        bottomBar = {
            if (current in bottomTabs.map { it.first.route }) {
                Row(Modifier.fillMaxWidth().background(Slovo.Card).border(2.5.dp, Slovo.Ink)) {
                    bottomTabs.forEach { (dest, label) ->
                        val active = current == dest.route
                        Box(
                            Modifier.weight(1f)
                                .background(if (active) Slovo.Yellow else Slovo.Card)
                                .clickable {
                                    nav.navigate(dest.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true; restoreState = true
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(label, color = Slovo.Ink, textAlign = TextAlign.Center,
                                 style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        },
    ) { pad ->
        NavHost(
            nav,
            startDestination = Dest.Learn.route,
            modifier = Modifier.padding(pad),
            // Tabs shouldn't slide like a push; a quick crossfade avoids the torn look on iOS.
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            composable(Dest.Learn.route) {
                HomeScreen(
                    module,
                    onOpenLesson = { unitId, lessonId -> nav.navigate(Dest.Lesson.of(unitId, lessonId)) },
                    onOpenDrill = { switchTab(nav, Dest.Drill.route) },
                )
            }
            composable(Dest.Drill.route) { DrillScreen(module, onOpenLearn = { switchTab(nav, Dest.Learn.route) }) }
            composable(Dest.League.route) { LeagueScreen() }
            composable(Dest.You.route) { YouScreen(module) }
            composable(Dest.Lesson.route) { back ->
                LessonScreen(
                    module = module,
                    unitId = back.arguments?.read { getStringOrNull("unitId") }.orEmpty(),
                    lessonId = back.arguments?.read { getStringOrNull("lessonId") }.orEmpty(),
                    onDone = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun switchTab(nav: androidx.navigation.NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
