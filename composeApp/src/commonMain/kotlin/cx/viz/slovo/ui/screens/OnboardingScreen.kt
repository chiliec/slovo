package cx.viz.slovo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cx.viz.slovo.domain.LearnUnit
import cx.viz.slovo.domain.LessonKind
import cx.viz.slovo.domain.PlacementCalculator
import cx.viz.slovo.domain.Question
import cx.viz.slovo.domain.QuestionFactory
import cx.viz.slovo.ui.AppModule
import cx.viz.slovo.ui.components.MishaButton
import cx.viz.slovo.ui.components.MishaCard
import cx.viz.slovo.ui.components.MishaMascot
import cx.viz.slovo.ui.theme.Slovo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val LEVEL_NEW = "new"

private enum class OnbStep { WELCOME, GOAL, LEVEL, DAILY_ORBIT, PLACEMENT, STREAK_COMMIT, READY }

private class OnboardingViewModel(private val module: AppModule) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var units: List<LearnUnit> = emptyList()

    var step by mutableStateOf(OnbStep.WELCOME); private set
    var goal by mutableStateOf(""); private set
    var level by mutableStateOf(""); private set
    var dailyMinutes by mutableStateOf(15); private set
    var placementQuestions by mutableStateOf<List<Question>>(emptyList()); private set
    var placementIndex by mutableStateOf(0); private set
    private var placementScore = 0
    private var startUnitId = ""

    init { scope.launch { units = module.content.units().map { module.content.unit(it.id) } } }

    fun startGoal() { step = OnbStep.GOAL }
    fun chooseGoal(g: String) { goal = g; step = OnbStep.LEVEL }
    fun chooseLevel(l: String) { level = l; step = OnbStep.DAILY_ORBIT }

    fun chooseDailyMinutes(minutes: Int) {
        dailyMinutes = minutes
        if (level == LEVEL_NEW || units.size < 2) {
            startUnitId = units.firstOrNull()?.meta?.id.orEmpty()
            step = OnbStep.STREAK_COMMIT
        } else {
            val pool = units.flatMap { it.cards }
            val factory = QuestionFactory(Random(pool.size + 3))
            placementQuestions = units.take(3).map { u ->
                factory.build(u.cards.first(), pool, LessonKind.VOCAB, isMastered = false)
            }
            placementIndex = 0; placementScore = 0
            step = OnbStep.PLACEMENT
        }
    }

    fun answerPlacement(optionIndex: Int) {
        val q = placementQuestions[placementIndex]
        if (optionIndex == q.correctIndex) placementScore++
        if (placementIndex + 1 < placementQuestions.size) {
            placementIndex++
        } else {
            val idx = PlacementCalculator.startUnitIndex(placementScore).coerceAtMost(units.size - 1)
            startUnitId = units.getOrNull(idx)?.meta?.id ?: units.firstOrNull()?.meta?.id.orEmpty()
            step = OnbStep.STREAK_COMMIT
        }
    }

    fun commitStreak() { step = OnbStep.READY }

    fun finish(onComplete: () -> Unit) {
        module.progress.completeOnboarding(goal, level, dailyMinutes, startUnitId)
        onComplete()
    }

    fun dispose() = scope.cancel()
}

@Composable
fun OnboardingScreen(module: AppModule, onComplete: () -> Unit) {
    val vm = remember { OnboardingViewModel(module) }
    DisposableEffect(vm) { onDispose { vm.dispose() } }
    when (vm.step) {
        OnbStep.WELCOME -> WelcomeStep(onNext = vm::startGoal)
        OnbStep.GOAL -> GoalStep(onChoose = vm::chooseGoal)
        OnbStep.LEVEL -> LevelStep(onChoose = vm::chooseLevel)
        OnbStep.DAILY_ORBIT -> DailyOrbitStep(onChoose = vm::chooseDailyMinutes)
        OnbStep.PLACEMENT -> PlacementStep(
            question = vm.placementQuestions[vm.placementIndex],
            index = vm.placementIndex,
            total = vm.placementQuestions.size,
            onAnswer = vm::answerPlacement,
        )
        OnbStep.STREAK_COMMIT -> StreakCommitStep(onNext = vm::commitStreak)
        OnbStep.READY -> ReadyStep(onNext = { vm.finish(onComplete) })
    }
}

@Composable private fun OnbScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable private fun WelcomeStep(onNext: () -> Unit) {
    OnbScaffold {
        Spacer(Modifier.weight(1f))
        MishaMascot(size = 96.dp)
        Text("WELCOME TO SLOVO", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink, textAlign = TextAlign.Center)
        Text("Learn real Russian phrases, a little every day.", color = Slovo.Ink,
             style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        MishaButton("LET'S GO →", Modifier.fillMaxWidth()) { onNext() }
    }
}

@Composable private fun GoalStep(onChoose: (String) -> Unit) {
    OnbScaffold {
        Text("WHY ARE YOU LEARNING RUSSIAN?", style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink, textAlign = TextAlign.Center)
        listOf("Travel" to "travel", "Family & friends" to "family", "Just for fun" to "fun").forEach { (label, value) ->
            OptionRow(label) { onChoose(value) }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable private fun LevelStep(onChoose: (String) -> Unit) {
    OnbScaffold {
        Text("HOW MUCH RUSSIAN DO YOU KNOW?", style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink, textAlign = TextAlign.Center)
        listOf("Just starting" to "new", "I know some phrases" to "some", "I'm conversational" to "fluent").forEach { (label, value) ->
            OptionRow(label) { onChoose(value) }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable private fun DailyOrbitStep(onChoose: (Int) -> Unit) {
    OnbScaffold {
        Text("SET YOUR DAILY GOAL", style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink, textAlign = TextAlign.Center)
        listOf(5, 10, 15, 20).forEach { minutes ->
            OptionRow("$minutes MIN/DAY  ·  ${minutes * 10} XP/DAY") { onChoose(minutes) }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable private fun PlacementStep(question: Question, index: Int, total: Int, onAnswer: (Int) -> Unit) {
    OnbScaffold {
        Text("${index + 1} / $total", color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium,
             modifier = Modifier.fillMaxWidth())
        Text(question.promptText, style = MaterialTheme.typography.titleMedium, color = Slovo.Ink, textAlign = TextAlign.Center)
        // The prompt no longer quotes the phrase, so placement has to show it like the quiz does.
        Text(question.card.russian, style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink, textAlign = TextAlign.Center)
        question.options.forEachIndexed { i, opt -> OptionRow(opt) { onAnswer(i) } }
        Spacer(Modifier.weight(1f))
    }
}

@Composable private fun StreakCommitStep(onNext: () -> Unit) {
    OnbScaffold {
        Spacer(Modifier.weight(1f))
        Text("KEEP YOUR STREAK ALIVE", style = MaterialTheme.typography.headlineMedium, color = Slovo.Ink, textAlign = TextAlign.Center)
        Text("A few minutes a day beats a big binge once a week.", color = Slovo.Ink,
             style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        MishaButton("I'M IN →", Modifier.fillMaxWidth()) { onNext() }
    }
}

@Composable private fun ReadyStep(onNext: () -> Unit) {
    OnbScaffold {
        Spacer(Modifier.weight(1f))
        MishaMascot(size = 96.dp)
        Text("READY?", style = MaterialTheme.typography.headlineLarge, color = Slovo.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        MishaButton("ПОЕХАЛИ!", Modifier.fillMaxWidth()) { onNext() }
    }
}

@Composable private fun OptionRow(label: String, onClick: () -> Unit) {
    MishaCard(Modifier.fillMaxWidth().clickable { onClick() }, shadow = 3.dp) {
        Text(label, Modifier.fillMaxWidth().padding(16.dp), color = Slovo.Ink, style = MaterialTheme.typography.titleMedium)
    }
}
