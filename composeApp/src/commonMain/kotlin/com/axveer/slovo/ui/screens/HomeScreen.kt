package com.axveer.slovo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axveer.slovo.domain.Lesson
import com.axveer.slovo.domain.LearnUnit
import com.axveer.slovo.domain.UserStats
import com.axveer.slovo.ui.AppModule
import com.axveer.slovo.ui.components.*
import com.axveer.slovo.ui.theme.Slovo
import kotlinx.coroutines.launch

private class HomeViewModel(private val module: AppModule) : ViewModel() {
    var unit by mutableStateOf<LearnUnit?>(null); private set
    var completed by mutableStateOf<Set<String>>(emptySet()); private set
    var stats by mutableStateOf(UserStats()); private set

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        val first = module.content.units().first()
        unit = module.content.unit(first.id)
        completed = module.progress.completedLessonIds()
        stats = module.progress.stats()
    }
    fun isUnlocked(lessons: List<Lesson>, index: Int): Boolean =
        index == 0 || lessons[index - 1].id in completed
}

@Composable
fun HomeScreen(module: AppModule, onOpenLesson: (String, String) -> Unit) {
    val vm = remember { HomeViewModel(module) }
    LaunchedEffect(Unit) { vm.refresh() }
    val unit = vm.unit ?: run { Text("Loading…", Modifier.padding(24.dp)); return }
    val lessons = unit.lessons
    val firstIncomplete = lessons.firstOrNull { it.id !in vm.completed }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("SLOVO", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink)
            MishaCard(shadow = 3.dp, background = Slovo.Bear) { MishaMascot(34.dp, Modifier.padding(4.dp)) }
        }
        MishaTicker("DAY ${vm.stats.streakDays} STREAK  ◆  ${vm.stats.xp} XP  ◆  LEAGUE COMING SOON  ◆  ")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MishaStatChip("${vm.stats.streakDays}", "DAY STREAK", Slovo.Card, Slovo.Ink, Modifier.weight(1f))
            MishaStatChip("${vm.stats.xp}", "XP", Slovo.Red, Slovo.Card, Modifier.weight(1f))
            MishaStatChip("SOON", "LEAGUE", Slovo.Blue, Slovo.Card, Modifier.weight(1f))
        }
        // UP NEXT
        if (firstIncomplete != null) {
            MishaCard(shadow = 5.dp) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("UP NEXT · ${unit.meta.title}".uppercase(),
                         style = MaterialTheme.typography.labelSmall, color = Slovo.Ink)
                    Text(firstIncomplete.title, style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink)
                    Text("${firstIncomplete.kind} · +30 XP", style = MaterialTheme.typography.bodyMedium, color = Slovo.Ink)
                    MishaButton("START →", Modifier.fillMaxWidth()) {
                        onOpenLesson(unit.meta.id, firstIncomplete.id)
                    }
                }
            }
        } else {
            MishaCard(shadow = 5.dp) { Text("Unit complete! 🎉", Modifier.padding(16.dp), color = Slovo.Ink) }
        }
        // lesson list
        lessons.forEachIndexed { i, lesson ->
            val done = lesson.id in vm.completed
            val unlocked = vm.isUnlocked(lessons, i)
            LessonRow(
                index = i + 1, title = lesson.title, subtitle = lesson.kind.name,
                done = done, current = lesson.id == firstIncomplete?.id, locked = !unlocked,
                onClick = { if (unlocked) onOpenLesson(unit.meta.id, lesson.id) },
            )
        }
    }
}
