package cx.viz.slovo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import slovo.composeapp.generated.resources.Res
import cx.viz.slovo.domain.ProfileSummary
import cx.viz.slovo.domain.SrsSnapshot
import cx.viz.slovo.domain.UnitMeta
import cx.viz.slovo.domain.UserStats
import cx.viz.slovo.platform.DebugClock
import cx.viz.slovo.platform.currentEpochDay
import cx.viz.slovo.platform.isDebugBuild
import cx.viz.slovo.ui.AppModule
import cx.viz.slovo.ui.components.Bar
import cx.viz.slovo.ui.components.MishaBarChart
import cx.viz.slovo.ui.components.MishaButton
import cx.viz.slovo.ui.components.MishaCard
import cx.viz.slovo.ui.components.MishaStatChip
import cx.viz.slovo.ui.theme.Slovo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private class ProfileViewModel(private val module: AppModule) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var stats by mutableStateOf(UserStats()); private set
    var unitPercents by mutableStateOf<List<Pair<UnitMeta, Int>>>(emptyList()); private set
    var srs by mutableStateOf<SrsSnapshot?>(null); private set
    var summary by mutableStateOf<ProfileSummary?>(null); private set
    init { reload() }
    // Same shape as Home/Drill: the tab is re-created on every visit, and the init-time
    // load alone doesn't survive the nav crossfade, so the screen also calls reload().
    fun reload() = scope.launch {
        stats = module.progress.stats()
        val metas = module.content.units()
        val learnUnits = metas.map { module.content.unit(it.id) }
        unitPercents = learnUnits.map { it.meta to module.progress.percent(it.cards.map { c -> c.id }) }
        val allIds = learnUnits.flatMap { it.cards }.map { it.id }.distinct()
        val lessonIds = learnUnits.flatMap { it.lessons }.map { it.id }.toSet()
        val lessonsCompleted = (module.progress.completedLessonIds() intersect lessonIds).size
        srs = module.progress.srsSnapshot(allIds, currentEpochDay())
        summary = module.progress.profileSummary(allIds, lessonsCompleted, lessonIds.size)
    }

    fun dispose() = scope.cancel()
}

@Composable fun YouScreen(module: AppModule, onOpenSettings: () -> Unit) {
    val vm = remember { ProfileViewModel(module) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    LaunchedEffect(Unit) { vm.reload() }
    var showCredits by remember { mutableStateOf(false) }
    if (showCredits) CreditsDialog(onDismiss = { showCredits = false })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("YOU", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink)
            MishaButton("SETTINGS", background = Slovo.Ink) { onOpenSettings() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MishaStatChip("${vm.stats.xp}", "TOTAL XP", Slovo.Red, Slovo.Card, Modifier.weight(1f))
            MishaStatChip("${vm.stats.streakDays}", "DAY STREAK", Slovo.Card, Slovo.Ink, Modifier.weight(1f))
        }
        vm.summary?.let { s ->
            Text("OVERVIEW", style = MaterialTheme.typography.labelSmall, color = Slovo.Ink)
            MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow("Accuracy", s.accuracyPercent?.let { "$it%" } ?: "—")
                    StatRow("Cards seen", "${s.cardsSeen}/${s.cardsTotal}")
                    StatRow("Cards mastered", "${s.cardsMastered}")
                    StatRow("Lessons done", "${s.lessonsCompleted}/${s.lessonsTotal}")
                    StatRow("Answers", "${s.totalAnswers}")
                }
            }
        }
        Text("MASTERY", style = MaterialTheme.typography.labelSmall, color = Slovo.Ink)
        vm.unitPercents.forEach { (meta, pct) ->
            MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(meta.title, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                    Text("$pct%", color = Slovo.Red, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        val srs = vm.srs
        if (srs != null) {
            Text("SPACED REPETITION", style = MaterialTheme.typography.labelSmall, color = Slovo.Ink,
                 modifier = Modifier.padding(top = 6.dp))
            if (srs.seenCount == 0) {
                MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp) {
                    Text("Study a lesson to start building your review deck.",
                         Modifier.padding(14.dp), color = Slovo.Ink,
                         style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("BOX STRENGTH", color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                        MishaBarChart(
                            bars = srs.boxCounts.mapIndexed { i, c -> Bar(i.toString(), c) },
                            barColor = Slovo.Red,
                        )
                        Text("new → mastered", color = Slovo.Ink.copy(alpha = 0.6f),
                             style = MaterialTheme.typography.bodyMedium)
                    }
                }
                MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DUE FORECAST", color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                        MishaBarChart(
                            bars = srs.dueForecast.mapIndexed { i, c -> Bar(if (i == 0) "NOW" else "+$i", c) },
                            barColor = Slovo.Blue,
                        )
                        Text("today … +6d", color = Slovo.Ink.copy(alpha = 0.6f),
                             style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Text("ABOUT", style = MaterialTheme.typography.labelSmall, color = Slovo.Ink,
             modifier = Modifier.padding(top = 6.dp))
        MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SLOVO", color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                Text("A gamified Russian words & phrases trainer.",
                     color = Slovo.Ink.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                Text("Audio: human recordings from Tatoeba (tatoeba.org), licensed CC-BY 4.0, " +
                     "© their respective contributors.",
                     color = Slovo.Ink.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                MishaButton("VIEW FULL CREDITS", background = Slovo.Ink) { showCredits = true }
            }
        }
        if (isDebugBuild) {
            var dayOffset by remember { mutableStateOf(DebugClock.dayOffset) }
            MishaButton("+1 DAY (debug) · now +$dayOffset", background = Slovo.Blue) {
                DebugClock.dayOffset += 1
                dayOffset = DebugClock.dayOffset
            }
        }
    }
}

@Composable private fun CreditsDialog(onDismiss: () -> Unit) {
    var text by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        text = runCatching { Res.readBytes("files/ATTRIBUTION.md").decodeToString() }
            .getOrDefault("Attribution data unavailable.")
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MishaCard(Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f), shadow = 4.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CREDITS", color = Slovo.Ink, style = MaterialTheme.typography.headlineMedium)
                Text(text ?: "Loading…", color = Slovo.Ink,
                     style = MaterialTheme.typography.bodyMedium,
                     modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()))
                MishaButton("CLOSE", background = Slovo.Red, modifier = Modifier.fillMaxWidth()) { onDismiss() }
            }
        }
    }
}

@Composable private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
        Text(value, color = Slovo.Red, style = MaterialTheme.typography.titleMedium)
    }
}
