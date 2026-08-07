package cx.viz.slovo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.viz.slovo.domain.AppSettings
import cx.viz.slovo.platform.Cue
import cx.viz.slovo.ui.AppModule
import cx.viz.slovo.ui.components.MishaButton
import cx.viz.slovo.ui.components.MishaCard
import cx.viz.slovo.ui.theme.Slovo
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(module: AppModule, onBack: () -> Unit) {
    var settings by remember { mutableStateOf(module.progress.settings()) }
    val scope = rememberCoroutineScope()
    fun update(next: AppSettings) {
        settings = next
        module.progress.updateSettings(next)
        if (next.hapticsEnabled) module.haptics.light()
        if (next.soundsEnabled) scope.launch { module.sound.play(Cue.SELECT) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("SETTINGS", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink)
            MishaButton("← BACK", background = Slovo.Ink) { onBack() }
        }
        SettingRow("SOUNDS", settings.soundsEnabled) { update(settings.copy(soundsEnabled = it)) }
        SettingRow("HAPTICS", settings.hapticsEnabled) { update(settings.copy(hapticsEnabled = it)) }
        SettingRow("NOTIFICATIONS", settings.notificationsEnabled) { update(settings.copy(notificationsEnabled = it)) }
    }
}

@Composable
private fun SettingRow(label: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
            MishaButton(
                if (on) "ON" else "OFF",
                background = if (on) Slovo.Blue else Slovo.Ink,
            ) { onToggle(!on) }
        }
    }
}
