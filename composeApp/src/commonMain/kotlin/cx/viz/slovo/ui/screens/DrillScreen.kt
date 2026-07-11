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
import cx.viz.slovo.platform.currentEpochDay
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

private enum class DrillPhase { LOADING, EMPTY, DRILL, RESULT }

private const val DRILL_SIZE = 10

private class DrillViewModel(private val module: AppModule) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var phase by mutableStateOf(DrillPhase.LOADING); private set
    var questions by mutableStateOf<List<Question>>(emptyList()); private set
    var index by mutableStateOf(0); private set
    var correctCount by mutableStateOf(0); private set
    var finalStats by mutableStateOf(UserStats()); private set
    var dueTotal by mutableStateOf(0); private set

    // A rotating seed so "DRILL AGAIN" reshuffles instead of repeating the same 10.
    private var round = 0

    init { load() }

    private fun load() = scope.launch {
        val all = module.content.allCards()
        val today = currentEpochDay()
        val allIds = all.map { it.id }
        val ids = module.progress.pickDrill(allIds, today, DRILL_SIZE)
        if (ids.isEmpty()) { phase = DrillPhase.EMPTY; return@launch }
        val byId = all.associateBy { it.id }
        val picked = ids.mapNotNull { byId[it] }
        val progress = module.progress.forCards(ids)
        val f = QuestionFactory(Random(picked.size + 1 + round * 31))
        questions = picked.map { c ->
            f.build(c, all, LessonKind.VOCAB, MasteryCalculator.isMastered(progress[c.id]), preferTyping = true)
        }
        dueTotal = module.progress.dueCount(allIds, today)
        index = 0; correctCount = 0; phase = DrillPhase.DRILL
    }

    fun answer(i: Int) {
        val q = questions[index]
        val correct = i == q.correctIndex
        if (correct) correctCount++
        module.progress.recordAnswer(q.card.id, correct, currentEpochDay())
        if (index + 1 < questions.size) index++ else finish()
    }

    fun recordTyped(correct: Boolean) {
        val q = questions[index]
        if (correct) correctCount++
        module.progress.recordAnswer(q.card.id, correct, currentEpochDay())
        if (index + 1 < questions.size) index++ else finish()
    }

    private fun finish() {
        finalStats = module.progress.recordDrillResult(correctCount)
        phase = DrillPhase.RESULT
    }

    fun drillAgain() { round++; phase = DrillPhase.LOADING; load() }
    fun reload() { if (phase != DrillPhase.DRILL) load() }
    fun play(file: String) = scope.launch { module.audio.play(file) }
    fun dispose() = scope.cancel()
}

@Composable fun DrillScreen(module: AppModule, onOpenLearn: () -> Unit) {
    val vm = remember { DrillViewModel(module) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    LaunchedEffect(Unit) { vm.reload() }
    when (vm.phase) {
        DrillPhase.LOADING -> Text("Loading…", Modifier.padding(24.dp))
        DrillPhase.EMPTY -> DrillEmptyView(onOpenLearn)
        DrillPhase.DRILL -> DrillQuestionView(vm)
        DrillPhase.RESULT -> DrillResultView(vm)
    }
}

@Composable private fun DrillEmptyView(onOpenLearn: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        Text("NOTHING TO REVIEW", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink, textAlign = TextAlign.Center)
        MishaCard(shadow = 5.dp) {
            Text("Finish a lesson first — cards you learn show up here for review.",
                 Modifier.padding(18.dp), color = Slovo.Ink, textAlign = TextAlign.Center,
                 style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        MishaButton("GO TO LEARN →", Modifier.fillMaxWidth()) { onOpenLearn() }
    }
}

@Composable private fun DrillQuestionView(vm: DrillViewModel) {
    val q = vm.questions[vm.index]
    if (q.mode == QuestionMode.TYPE) {
        cx.viz.slovo.ui.components.TypedQuestionContent(
            question = q,
            header = "DRILL · ${vm.index + 1}/${vm.questions.size}  ·  ${vm.dueTotal} DUE",
            onPlay = { vm.play(it) },
            onContinue = { vm.recordTyped(it) },
        )
        return
    }
    var chosen by remember(vm.index) { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DRILL · ${vm.index + 1}/${vm.questions.size}  ·  ${vm.dueTotal} DUE",
                 color = Slovo.Ink, style = MaterialTheme.typography.labelSmall)
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
            val last = vm.index + 1 == vm.questions.size
            MishaButton(if (last) "FINISH →" else "NEXT →", Modifier.fillMaxWidth()) { vm.answer(chosen!!) }
        }
    }
}

@Composable private fun DrillResultView(vm: DrillViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        Text("DRILL DONE", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink)
        MishaCard(shadow = 5.dp, background = Slovo.Yellow) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+${XpCalculator.drillXp(vm.correctCount)} XP",
                     style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink)
                Text("${vm.correctCount} / ${vm.questions.size} correct", color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                Text("${vm.finalStats.xp} XP total", color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.weight(1f))
        MishaButton("DRILL AGAIN", Modifier.fillMaxWidth()) { vm.drillAgain() }
    }
}
