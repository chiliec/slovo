package cx.viz.slovo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.viz.slovo.domain.LearnUnit
import cx.viz.slovo.domain.Lesson
import cx.viz.slovo.domain.UserStats
import cx.viz.slovo.platform.currentEpochDay
import cx.viz.slovo.ui.AppModule
import cx.viz.slovo.ui.components.*
import cx.viz.slovo.ui.theme.Slovo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private class HomeViewModel(private val module: AppModule) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var units by mutableStateOf<List<LearnUnit>>(emptyList()); private set
    var completed by mutableStateOf<Set<String>>(emptySet()); private set
    var stats by mutableStateOf(UserStats()); private set
    var dueCount by mutableStateOf(0); private set

    init { refresh() }
    fun refresh() = scope.launch {
        units = module.content.units().map { module.content.unit(it.id) }
        completed = module.progress.completedLessonIds()
        stats = module.progress.stats()
        val allIds = units.flatMap { it.cards }.map { it.id }.distinct()
        dueCount = module.progress.dueCount(allIds, currentEpochDay())
    }

    fun dispose() = scope.cancel()
}

/** One lesson placed in the global, cross-unit study sequence. */
private data class FlatLesson(val unit: LearnUnit, val lesson: Lesson, val position: Int)

@Composable
fun HomeScreen(module: AppModule, onOpenLesson: (String, String) -> Unit, onOpenDrill: () -> Unit) {
    val vm = remember { HomeViewModel(module) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    LaunchedEffect(Unit) { vm.refresh() }
    val units = vm.units
    if (units.isEmpty()) { Text("Loading…", Modifier.padding(24.dp)); return }

    // Flatten every unit's lessons into one ordered sequence so unlocking carries across units:
    // a lesson opens once the lesson directly before it (even in the previous unit) is complete.
    val flat = units.flatMap { u -> u.lessons.map { u to it } }
        .mapIndexed { pos, (u, l) -> FlatLesson(u, l, pos) }
    val firstIncomplete = flat.firstOrNull { it.lesson.id !in vm.completed }
    fun unlocked(pos: Int): Boolean = pos == 0 || flat[pos - 1].lesson.id in vm.completed

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("SLOVO", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink)
            MishaCard(shadow = 3.dp, background = Slovo.Mascot) { MishaMascot(34.dp, Modifier.padding(4.dp)) }
        }
        val duePart = if (vm.dueCount > 0) "${vm.dueCount} CARDS DUE  ◆  " else ""
        MishaTicker("${duePart}DAY ${vm.stats.streakDays} STREAK  ◆  ${vm.stats.xp} XP  ◆  LEAGUE COMING SOON  ◆  ")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MishaStatChip("${vm.stats.streakDays}", "DAY STREAK", Slovo.Card, Slovo.Ink, Modifier.weight(1f))
            MishaStatChip("${vm.stats.xp}", "XP", Slovo.Red, Slovo.Card, Modifier.weight(1f))
            MishaStatChip("SOON", "LEAGUE", Slovo.Blue, Slovo.Card, Modifier.weight(1f))
        }
        if (vm.dueCount > 0) {
            MishaCard(shadow = 5.dp, background = Slovo.Yellow) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${vm.dueCount} CARDS DUE", style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink)
                    Text("Keep your words fresh.", style = MaterialTheme.typography.bodyMedium, color = Slovo.Ink)
                    MishaButton("REVIEW NOW →", Modifier.fillMaxWidth()) { onOpenDrill() }
                }
            }
        }
        // UP NEXT — the first incomplete lesson anywhere in the sequence
        if (firstIncomplete != null) {
            MishaCard(shadow = 5.dp) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("UP NEXT · ${firstIncomplete.unit.meta.title}".uppercase(),
                         style = MaterialTheme.typography.labelSmall, color = Slovo.Ink)
                    Text(firstIncomplete.lesson.title, style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink)
                    Text("${firstIncomplete.lesson.kind} · +30 XP", style = MaterialTheme.typography.bodyMedium, color = Slovo.Ink)
                    MishaButton("START →", Modifier.fillMaxWidth()) {
                        onOpenLesson(firstIncomplete.unit.meta.id, firstIncomplete.lesson.id)
                    }
                }
            }
        } else {
            MishaCard(shadow = 5.dp) { Text("All units complete! 🎉", Modifier.padding(16.dp), color = Slovo.Ink) }
        }
        // Unit sections — each unit's title followed by its lessons, numbered across the whole course
        units.forEachIndexed { uIdx, unit ->
            Text("UNIT ${uIdx + 1} · ${unit.meta.title}".uppercase(),
                 style = MaterialTheme.typography.labelSmall, color = Slovo.Ink,
                 modifier = Modifier.padding(top = 6.dp))
            unit.lessons.forEach { lesson ->
                val pos = flat.first { it.lesson.id == lesson.id }.position
                val open = unlocked(pos)
                LessonRow(
                    index = pos + 1, title = lesson.title, subtitle = lesson.kind.name,
                    done = lesson.id in vm.completed,
                    current = lesson.id == firstIncomplete?.lesson?.id, locked = !open,
                    onClick = { if (open) onOpenLesson(unit.meta.id, lesson.id) },
                )
            }
        }
    }
}
