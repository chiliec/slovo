package cx.viz.slovo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import cx.viz.slovo.platform.Cue
import cx.viz.slovo.platform.currentEpochDay
import cx.viz.slovo.ui.AppModule
import cx.viz.slovo.ui.playCue
import cx.viz.slovo.ui.components.MishaButton
import cx.viz.slovo.ui.components.MishaCard
import cx.viz.slovo.ui.components.MishaStatChip
import cx.viz.slovo.ui.theme.Slovo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class Phase { LOADING, STUDY, QUIZ, MISTAKE_REVIEW, MISTAKE_DRILL, OUT_OF_HEARTS, RESULT }

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
    var hearts by mutableStateOf(Hearts.MAX); private set
    var mistakes by mutableStateOf<List<Card>>(emptyList()); private set
    var drillQuestions by mutableStateOf<List<Question>>(emptyList()); private set
    var drillIndex by mutableStateOf(0); private set
    var drillCorrect by mutableStateOf(0); private set
    var finalStats by mutableStateOf(UserStats()); private set
    var showQuitConfirm by mutableStateOf(false); private set

    fun requestQuit() { showQuitConfirm = true }
    fun cancelQuit() { showQuitConfirm = false }

    fun chipAdded() { scope.launch { module.playCue(Cue.SELECT) } }

    init { scope.launch {
        val unit = module.content.unit(unitId)
        val lesson = unit.lessons.first { it.id == lessonId }
        kind = lesson.kind
        cards = lesson.cardIds.mapNotNull { id -> unit.cards.firstOrNull { it.id == id } }
        phase = Phase.STUDY
    } }

    /** Fixed step sequence: word-bank warm-up, per-card mastery-gated MC/type, a pair-match round, a speaking step. */
    fun startQuiz() {
        val progress = module.progress.forCards(cards.map { it.id })
        val factory = QuestionFactory(Random(cards.size + 7))
        val mc = cards.map { c -> factory.build(c, cards, kind, MasteryCalculator.isMastered(progress[c.id])) }
        questions = buildList {
            add(factory.buildWordBank(cards.first(), cards))
            addAll(mc)
            if (cards.size >= 3) add(factory.buildPairMatch(cards))
            add(factory.buildSpeak(cards.last()))
        }
        index = 0; correctCount = 0; hearts = Hearts.MAX; mistakes = emptyList(); phase = Phase.QUIZ
    }

    fun answer(optionIndex: Int) {
        val q = questions[index]
        val correct = optionIndex == q.correctIndex
        record(q, correct)
    }

    fun recordTyped(correct: Boolean) {
        val q = questions[index]
        record(q, correct)
    }

    fun pairMismatch() {
        hearts = Hearts.afterMiss(hearts)
        scope.launch { module.playCue(Cue.ERROR) }
        if (Hearts.isDepleted(hearts)) phase = Phase.OUT_OF_HEARTS
    }

    fun pairMatchComplete() {
        val q = questions[index]
        correctCount++
        q.pairCards.forEach { module.progress.recordAnswer(it.id, true, currentEpochDay()) }
        scope.launch { module.playCue(Cue.PAIR_MATCH) }
        advance()
    }

    fun speakComplete() = record(questions[index], true)

    private fun record(q: Question, correct: Boolean) {
        if (correct) correctCount++ else { hearts = Hearts.afterMiss(hearts); mistakes = mistakes + q.card }
        module.progress.recordAnswer(q.card.id, correct, currentEpochDay())
        scope.launch { module.playCue(if (correct) Cue.SUCCESS else Cue.ERROR) }
        if (Hearts.isDepleted(hearts)) phase = Phase.OUT_OF_HEARTS else advance()
    }

    private fun advance() {
        if (index + 1 < questions.size) index++ else finish()
    }

    private fun finish() {
        if (mistakes.isNotEmpty()) phase = Phase.MISTAKE_REVIEW else completeAndCelebrate()
    }

    fun startMistakeDrill() {
        val distinctMistakes = mistakes.distinctBy { it.id }
        val factory = QuestionFactory(Random(distinctMistakes.size + 11))
        drillQuestions = distinctMistakes.map { c -> factory.build(c, cards, kind, isMastered = false) }
        drillIndex = 0; drillCorrect = 0
        phase = Phase.MISTAKE_DRILL
    }

    fun answerMistakeDrill(correct: Boolean) {
        if (correct) drillCorrect++
        if (drillIndex + 1 < drillQuestions.size) drillIndex++ else completeAndCelebrate()
    }

    fun skipMistakeReview() = completeAndCelebrate()

    private fun completeAndCelebrate() {
        finalStats = module.progress.completeLesson(lessonId, correctCount, currentEpochDay())
        scope.launch { module.playCue(Cue.LESSON_COMPLETE) }
        phase = Phase.RESULT
    }

    fun playAudio(file: String) = scope.launch { module.audio.play(file) }
    fun dispose() = scope.cancel()
}

@Composable
fun LessonScreen(module: AppModule, unitId: String, lessonId: String, onDone: () -> Unit) {
    val vm = remember { LessonViewModel(module, unitId, lessonId) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    Box(Modifier.fillMaxSize()) {
        when (vm.phase) {
            Phase.LOADING -> Text("Loading…", Modifier.padding(24.dp))
            Phase.STUDY -> StudyView(vm, onPractice = vm::startQuiz)
            Phase.QUIZ -> QuizView(vm)
            Phase.MISTAKE_REVIEW -> MistakeReviewView(vm)
            Phase.MISTAKE_DRILL -> MistakeDrillView(vm)
            Phase.OUT_OF_HEARTS -> OutOfHeartsView(onDone)
            Phase.RESULT -> ResultView(vm, onDone)
        }
        if (vm.phase == Phase.STUDY || vm.phase == Phase.QUIZ) {
            Text(
                "✕", color = Slovo.Ink, style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).clickable { vm.requestQuit() },
            )
        }
        if (vm.showQuitConfirm) QuitConfirmOverlay(onKeepGoing = vm::cancelQuit, onQuit = onDone)
    }
}

@Composable private fun QuitConfirmOverlay(onKeepGoing: () -> Unit, onQuit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Slovo.Ink.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        MishaCard(Modifier.padding(32.dp), shadow = 5.dp) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                   verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DON'T FLOAT AWAY", style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink, textAlign = TextAlign.Center)
                Text("Your progress in this lesson will be lost.", color = Slovo.Ink,
                     style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                MishaButton("KEEP GOING", Modifier.fillMaxWidth()) { onKeepGoing() }
                MishaButton("QUIT LESSON", Modifier.fillMaxWidth(), background = Slovo.Red) { onQuit() }
            }
        }
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
    when (q.mode) {
        QuestionMode.TYPE -> cx.viz.slovo.ui.components.TypedQuestionContent(
            question = q,
            header = "${vm.index + 1} / ${vm.questions.size}",
            hearts = vm.hearts,
            onPlay = { vm.playAudio(it) },
            onContinue = { vm.recordTyped(it) },
        )
        QuestionMode.WORD_BANK -> WordBankView(vm, q)
        QuestionMode.PAIR_MATCH -> PairMatchView(vm, q)
        QuestionMode.SPEAK -> SpeakView(vm, q)
        else -> McQuestionBody(
            q = q,
            headerContent = { cx.viz.slovo.ui.components.QuizHeader(vm.index, vm.questions.size, vm.hearts) },
            onPlay = { vm.playAudio(it) },
            onAnswer = { vm.answer(it) },
        )
    }
}

/** Shared prompt-card + options body for LISTEN/READ/PRODUCE questions and mistake-drill MC steps. */
@Composable private fun McQuestionBody(
    q: Question,
    headerContent: @Composable () -> Unit,
    onPlay: (String) -> Unit,
    onAnswer: (Int) -> Unit,
) {
    var chosen by remember(q) { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            headerContent()
            MishaCard(Modifier.fillMaxWidth(), shadow = 5.dp) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(q.promptText, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    if (q.mode == QuestionMode.LISTEN) MishaButton("🔊 PLAY", background = Slovo.Blue) { onPlay(q.card.audio) }
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
            MishaButton("CONTINUE →", Modifier.fillMaxWidth()) { onAnswer(chosen!!) }
        }
    }
}

@Composable private fun WordBankView(vm: LessonViewModel, q: Question) {
    var selected by remember(vm.index) { mutableStateOf<List<Int>>(emptyList()) }
    var result by remember(vm.index) { mutableStateOf<AnswerChecker.Result?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cx.viz.slovo.ui.components.QuizHeader(vm.index, vm.questions.size, vm.hearts)
            MishaCard(Modifier.fillMaxWidth(), shadow = 5.dp) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(q.promptText, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
            }
            Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(Slovo.Card).border(2.5.dp, Slovo.Ink).padding(10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selected.forEach { i ->
                        WordChip(q.options[i]) { if (result == null) selected = selected - i }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                q.options.indices.filter { it !in selected }.forEach { i ->
                    WordChip(q.options[i]) { if (result == null) { selected = selected + i; vm.chipAdded() } }
                }
            }
            result?.let { r ->
                val bg = if (r.verdict != AnswerChecker.Verdict.WRONG) Slovo.Blue else Slovo.Red
                MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp, background = bg) {
                    Text(if (r.verdict != AnswerChecker.Verdict.WRONG) "Correct!" else "Answer: ${r.canonical}",
                         Modifier.fillMaxWidth().padding(14.dp), color = Slovo.Card, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (result == null) {
            MishaButton("CHECK", Modifier.fillMaxWidth(), enabled = selected.isNotEmpty()) {
                val assembled = selected.joinToString(" ") { q.options[it] }
                result = AnswerChecker.check(assembled, q.card.english)
            }
        } else {
            MishaButton("CONTINUE →", Modifier.fillMaxWidth()) { vm.recordTyped(result!!.verdict != AnswerChecker.Verdict.WRONG) }
        }
    }
}

@Composable private fun WordChip(text: String, onClick: () -> Unit) {
    MishaCard(Modifier.clickable(onClick = onClick), shadow = 2.dp) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable private fun PairMatchView(vm: LessonViewModel, q: Question) {
    val pairs = q.pairCards
    val ruOrder = remember(vm.index) { pairs.shuffled() }
    val enOrder = remember(vm.index) { pairs.shuffled() }
    var matched by remember(vm.index) { mutableStateOf(setOf<String>()) }
    var selectedRuId by remember(vm.index) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cx.viz.slovo.ui.components.QuizHeader(vm.index, vm.questions.size, vm.hearts)
        Text(q.promptText, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
             modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ruOrder.forEach { c ->
                    val isMatched = c.id in matched
                    MishaCard(
                        Modifier.fillMaxWidth().let { if (!isMatched) it.clickable { selectedRuId = c.id } else it },
                        shadow = 3.dp,
                        background = when { isMatched -> Slovo.Blue; c.id == selectedRuId -> Slovo.Yellow; else -> Slovo.Card },
                    ) {
                        Text(c.russian, Modifier.fillMaxWidth().padding(12.dp),
                             color = if (isMatched) Slovo.Card else Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                enOrder.forEach { c ->
                    val isMatched = c.id in matched
                    MishaCard(
                        Modifier.fillMaxWidth().let {
                            if (!isMatched) it.clickable {
                                val ru = selectedRuId
                                if (ru != null) {
                                    if (ru == c.id) {
                                        matched = matched + c.id
                                        selectedRuId = null
                                        if (matched.size == pairs.size) vm.pairMatchComplete()
                                    } else {
                                        vm.pairMismatch()
                                        selectedRuId = null
                                    }
                                }
                            } else it
                        },
                        shadow = 3.dp,
                        background = if (isMatched) Slovo.Blue else Slovo.Card,
                    ) {
                        Text(c.english, Modifier.fillMaxWidth().padding(12.dp),
                             color = if (isMatched) Slovo.Card else Slovo.Ink, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable private fun SpeakView(vm: LessonViewModel, q: Question) {
    var recording by remember(vm.index) { mutableStateOf(false) }
    LaunchedEffect(recording) {
        if (recording) { delay(1600); vm.speakComplete() }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cx.viz.slovo.ui.components.QuizHeader(vm.index, vm.questions.size, vm.hearts)
        Spacer(Modifier.weight(1f))
        MishaCard(Modifier.fillMaxWidth(), shadow = 5.dp) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally,
                   verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(q.card.russian, style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink, textAlign = TextAlign.Center)
                Text(q.promptText, color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
            }
        }
        MishaButton(if (recording) "LISTENING…" else "🎤 SPEAK", Modifier.fillMaxWidth(), enabled = !recording) { recording = true }
        MishaButton("CAN'T SPEAK RIGHT NOW", Modifier.fillMaxWidth(), background = Slovo.Ink, enabled = !recording) { vm.speakComplete() }
        Spacer(Modifier.weight(1f))
    }
}

@Composable private fun MistakeReviewView(vm: LessonViewModel) {
    val count = vm.mistakes.distinctBy { it.id }.size
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        Text("$count TO PATCH UP", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink, textAlign = TextAlign.Center)
        Text("Drill your mistakes for +10 XP, or skip and finish up.", color = Slovo.Ink,
             style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        MishaButton("DRILL MISTAKES +10 XP", Modifier.fillMaxWidth()) { vm.startMistakeDrill() }
        MishaButton("SKIP", Modifier.fillMaxWidth(), background = Slovo.Ink) { vm.skipMistakeReview() }
    }
}

@Composable private fun MistakeDrillView(vm: LessonViewModel) {
    val q = vm.drillQuestions[vm.drillIndex]
    val header = "${vm.drillIndex + 1} / ${vm.drillQuestions.size}"
    if (q.mode == QuestionMode.TYPE) {
        cx.viz.slovo.ui.components.TypedQuestionContent(
            question = q, header = header,
            onPlay = { vm.playAudio(it) },
            onContinue = { vm.answerMistakeDrill(it) },
        )
    } else {
        McQuestionBody(
            q = q,
            headerContent = { Text(header, color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium) },
            onPlay = { vm.playAudio(it) },
            onAnswer = { i -> vm.answerMistakeDrill(i == q.correctIndex) },
        )
    }
}

@Composable private fun OutOfHeartsView(onExit: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        Text("OUT OF HEARTS", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink, textAlign = TextAlign.Center)
        Text("Come back and try this lesson again.", color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        MishaButton("BACK TO HOME", Modifier.fillMaxWidth()) { onExit() }
    }
}

@Composable private fun ResultView(vm: LessonViewModel, onDone: () -> Unit) {
    val accuracy = if (vm.questions.isEmpty()) 0 else vm.correctCount * 100 / vm.questions.size
    Box(Modifier.fillMaxSize().background(Slovo.Blue)) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.weight(1f))
            Text("LIFTOFF!", style = MaterialTheme.typography.headlineLarge, color = Slovo.Card)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MishaStatChip(
                    value = "+${XpCalculator.sessionXp(vm.correctCount)}", label = "XP EARNED",
                    background = Slovo.Yellow, textColor = Slovo.Ink, modifier = Modifier.weight(1f),
                )
                MishaStatChip(
                    value = "$accuracy%", label = "ACCURACY",
                    background = Slovo.Card, textColor = Slovo.Ink, modifier = Modifier.weight(1f),
                )
            }
            MishaCard(shadow = 3.dp, background = Slovo.Yellow) {
                Text("DAY ${vm.finalStats.streakDays} — STREAK SAFE", Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                     color = Slovo.Ink, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.weight(1f))
            MishaButton("CONTINUE", Modifier.fillMaxWidth()) { onDone() }
        }
    }
}
