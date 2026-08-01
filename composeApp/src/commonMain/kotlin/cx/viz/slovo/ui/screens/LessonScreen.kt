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

private enum class Phase { LOADING, STUDY, QUIZ, RESULT }

private class LessonViewModel(
    private val module: AppModule, private val unitId: String, private val lessonId: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var phase by mutableStateOf(Phase.LOADING); private set
    var cards by mutableStateOf<List<Card>>(emptyList()); private set
    var kind by mutableStateOf(LessonKind.VOCAB); private set
    var index by mutableStateOf(0); private set
    var questions by mutableStateOf<List<Question>>(emptyList()); private set
    var correctCount by mutableStateOf(0); private set
    var finalStats by mutableStateOf(UserStats()); private set

    init { scope.launch {
        val unit = module.content.unit(unitId)
        val lesson = unit.lessons.first { it.id == lessonId }
        kind = lesson.kind
        cards = lesson.cardIds.mapNotNull { id -> unit.cards.firstOrNull { it.id == id } }
        phase = Phase.STUDY
    } }

    fun startQuiz() {
        val progress = module.progress.forCards(cards.map { it.id })
        val factory = QuestionFactory(Random(cards.size + 7))
        questions = cards.map { c ->
            factory.build(c, cards, kind, MasteryCalculator.isMastered(progress[c.id]))
        }
        index = 0; correctCount = 0; phase = Phase.QUIZ
    }

    fun answer(optionIndex: Int) {
        val q = questions[index]
        val correct = optionIndex == q.correctIndex
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
        finalStats = module.progress.completeLesson(lessonId, correctCount, currentEpochDay())
        phase = Phase.RESULT
    }

    fun playAudio(file: String) = scope.launch { module.audio.play(file) }
    fun dispose() = scope.cancel()
}

@Composable
fun LessonScreen(module: AppModule, unitId: String, lessonId: String, onDone: () -> Unit) {
    val vm = remember { LessonViewModel(module, unitId, lessonId) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    when (vm.phase) {
        Phase.LOADING -> Text("Loading…", Modifier.padding(24.dp))
        Phase.STUDY -> StudyView(vm, onPractice = vm::startQuiz)
        Phase.QUIZ -> QuizView(vm)
        Phase.RESULT -> ResultView(vm, onDone)
    }
}

@Composable private fun StudyView(vm: LessonViewModel, onPractice: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    var showTranslit by remember { mutableStateOf(true) }
    val card = vm.cards[page]
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${page + 1} / ${vm.cards.size}", color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium)
            MishaCard(Modifier.fillMaxWidth(), shadow = 5.dp) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                       verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(card.russian, style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink, textAlign = TextAlign.Center)
                    if (showTranslit) Text(card.transliteration, color = Slovo.Red, style = MaterialTheme.typography.titleMedium)
                    Text(card.english, color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium)
                    MishaButton("🔊 PLAY", background = Slovo.Blue) { vm.playAudio(card.audio) }
                }
            }
            Text("translit: ${if (showTranslit) "on" else "off"}",
                 color = Slovo.Ink, modifier = Modifier.clickable { showTranslit = !showTranslit },
                 style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (page > 0) MishaButton("← BACK", Modifier.weight(1f), Slovo.Card) { page-- }
            if (page + 1 < vm.cards.size) MishaButton("NEXT →", Modifier.weight(1f)) { page++ }
            else MishaButton("PRACTICE", Modifier.weight(1f)) { onPractice() }
        }
    }
}

@Composable private fun QuizView(vm: LessonViewModel) {
    val q = vm.questions[vm.index]
    if (q.mode == QuestionMode.TYPE) {
        cx.viz.slovo.ui.components.TypedQuestionContent(
            question = q,
            header = "${vm.index + 1} / ${vm.questions.size}",
            onPlay = { vm.playAudio(it) },
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
            Text("${vm.index + 1} / ${vm.questions.size}", color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium)
            MishaCard(Modifier.fillMaxWidth(), shadow = 5.dp) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(q.promptText, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    if (q.mode == QuestionMode.LISTEN) MishaButton("🔊 PLAY", background = Slovo.Blue) { vm.playAudio(q.card.audio) }
                    else if (q.mode == QuestionMode.READ) Text(q.card.russian, style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink)
                }
            }
            q.options.forEachIndexed { i, opt ->
                val bg = when {
                    chosen == null -> Slovo.Card
                    i == q.correctIndex -> Slovo.Blue
                    i == chosen -> Slovo.Red
                    else -> Slovo.Card
                }
                MishaCard(Modifier.fillMaxWidth().let {
                    if (chosen == null) it.clickable { chosen = i } else it
                }, shadow = 3.dp, background = bg) {
                    Text(opt, Modifier.fillMaxWidth().padding(14.dp),
                         color = if (chosen != null && (i == q.correctIndex || i == chosen)) Slovo.Card else Slovo.Ink,
                         style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (chosen != null) {
            Spacer(Modifier.height(12.dp))
            MishaButton("CONTINUE →", Modifier.fillMaxWidth()) { vm.answer(chosen!!) }
        }
    }
}

@Composable private fun ResultView(vm: LessonViewModel, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        Text("LESSON DONE", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink)
        MishaCard(shadow = 5.dp, background = Slovo.Yellow) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+${cx.viz.slovo.domain.XpCalculator.sessionXp(vm.correctCount)} XP",
                     style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink)
                Text("${vm.correctCount} / ${vm.questions.size} correct", color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                Text("🔥 ${vm.finalStats.streakDays} day streak", color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.weight(1f))
        MishaButton("DONE", Modifier.fillMaxWidth()) { onDone() }
    }
}
