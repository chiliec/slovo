package cx.viz.slovo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.viz.slovo.domain.UnitMeta
import cx.viz.slovo.domain.UserStats
import cx.viz.slovo.platform.DebugClock
import cx.viz.slovo.platform.isDebugBuild
import cx.viz.slovo.ui.AppModule
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
    init { scope.launch {
        stats = module.progress.stats()
        unitPercents = module.content.units().map { meta ->
            val cardIds = module.content.unit(meta.id).cards.map { it.id }
            meta to module.progress.percent(cardIds)
        }
    } }

    fun dispose() = scope.cancel()
}

@Composable fun YouScreen(module: AppModule) {
    val vm = remember { ProfileViewModel(module) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("YOU", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MishaStatChip("${vm.stats.xp}", "TOTAL XP", Slovo.Red, Slovo.Card, Modifier.weight(1f))
            MishaStatChip("${vm.stats.streakDays}", "DAY STREAK", Slovo.Card, Slovo.Ink, Modifier.weight(1f))
        }
        Text("MASTERY", style = MaterialTheme.typography.labelSmall, color = Slovo.Ink)
        vm.unitPercents.forEach { (meta, pct) ->
            MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(meta.title, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                    Text("$pct%", color = Slovo.Red, style = MaterialTheme.typography.titleMedium)
                }
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
