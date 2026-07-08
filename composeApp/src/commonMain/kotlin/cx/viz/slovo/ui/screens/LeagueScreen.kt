package cx.viz.slovo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.viz.slovo.ui.components.MishaCard
import cx.viz.slovo.ui.components.MishaMascot
import cx.viz.slovo.ui.theme.Slovo

@Composable fun LeagueScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        MishaMascot(96.dp)
        MishaCard(shadow = 5.dp, background = Slovo.Blue) {
            Text("LEAGUES — COMING SOON", Modifier.padding(20.dp), color = Slovo.Card,
                 style = MaterialTheme.typography.headlineMedium)
        }
        Text("Compete with other learners in a future update.", color = Slovo.Ink,
             style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
    }
}
