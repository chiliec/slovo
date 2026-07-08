package cx.viz.slovo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cx.viz.slovo.domain.*
import cx.viz.slovo.ui.AppModule
import cx.viz.slovo.ui.components.MishaButton
import cx.viz.slovo.ui.components.MishaCard
import cx.viz.slovo.ui.theme.Slovo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.random.Random

private class DrillViewModel(private val module: AppModule) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var questions by mutableStateOf<List<Question>>(emptyList()); private set
    var index by mutableStateOf(0); private set
    init { scope.launch {
        val all = module.content.allCards()
        val progress = module.progress.forCards(all.map { it.id })
        val f = QuestionFactory(Random(all.size + 1))
        questions = all.shuffled(Random(all.size + 2)).take(10).map { c ->
            f.build(c, all, LessonKind.VOCAB, MasteryCalculator.isMastered(progress[c.id]))
        }
    } }
    fun answer(i: Int) {
        val q = questions[index]
        module.progress.recordAnswer(q.card.id, i == q.correctIndex)
        if (index + 1 < questions.size) index++ else index = 0 // loop for MVP
    }
    fun play(file: String) = scope.launch { module.audio.play(file) }
    fun dispose() = scope.cancel()
}

@Composable fun DrillScreen(module: AppModule) {
    val vm = remember { DrillViewModel(module) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    if (vm.questions.isEmpty()) { Text("Loading…", Modifier.padding(24.dp)); return }
    val q = vm.questions[vm.index]
    var chosen by remember(vm.index) { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DRILL · ${vm.index + 1}/${vm.questions.size}", color = Slovo.Ink, style = MaterialTheme.typography.labelSmall)
            MishaCard(Modifier.fillMaxWidth(), shadow = 5.dp) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(q.promptText, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    if (q.mode == QuestionMode.LISTEN) MishaButton("🔊 PLAY", background = Slovo.Blue) { vm.play(q.card.audio!!) }
                    else if (q.mode == QuestionMode.READ) Text(q.card.russian, style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink)
                }
            }
            q.options.forEachIndexed { i, opt ->
                val bg = when { chosen == null -> Slovo.Card; i == q.correctIndex -> Slovo.Blue; i == chosen -> Slovo.Red; else -> Slovo.Card }
                MishaCard(Modifier.fillMaxWidth().let { if (chosen == null) it.clickable { chosen = i } else it }, shadow = 3.dp, background = bg) {
                    Text(opt, Modifier.padding(14.dp),
                         color = if (chosen != null && (i == q.correctIndex || i == chosen)) Slovo.Card else Slovo.Ink,
                         style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (chosen != null) {
            Spacer(Modifier.height(12.dp))
            MishaButton("NEXT →", Modifier.fillMaxWidth()) { vm.answer(chosen!!) }
        }
    }
}
