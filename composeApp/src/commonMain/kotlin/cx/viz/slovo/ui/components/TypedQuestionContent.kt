package cx.viz.slovo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cx.viz.slovo.domain.AnswerChecker
import cx.viz.slovo.domain.Question
import cx.viz.slovo.ui.theme.Slovo

@Composable
fun TypedQuestionContent(
    question: Question,
    header: String,
    onPlay: (String) -> Unit,
    onContinue: (correct: Boolean) -> Unit,
) {
    var text by remember(question) { mutableStateOf("") }
    var result by remember(question) { mutableStateOf<AnswerChecker.Result?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(header, color = Slovo.Ink, style = MaterialTheme.typography.labelSmall)
            MishaCard(Modifier.fillMaxWidth(), shadow = 5.dp) {
                Column(
                    Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(question.promptText, color = Slovo.Ink,
                         style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(question.card.russian, color = Slovo.Ink,
                         style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    question.audio?.let { MishaButton("🔊 PLAY", background = Slovo.Blue) { onPlay(it) } }
                }
            }

            if (result == null) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your answer") },
                )
            } else {
                val r = result!!
                val bg = when (r.verdict) {
                    AnswerChecker.Verdict.CORRECT -> Slovo.Blue
                    AnswerChecker.Verdict.ALMOST -> Slovo.Yellow
                    AnswerChecker.Verdict.WRONG -> Slovo.Red
                }
                val msg = when (r.verdict) {
                    AnswerChecker.Verdict.CORRECT -> "Correct!"
                    AnswerChecker.Verdict.ALMOST -> "Almost — it's: ${r.canonical}"
                    AnswerChecker.Verdict.WRONG -> "Answer: ${r.canonical}"
                }
                MishaCard(Modifier.fillMaxWidth(), shadow = 3.dp, background = bg) {
                    Text(msg, Modifier.padding(14.dp),
                         color = if (r.verdict == AnswerChecker.Verdict.ALMOST) Slovo.Ink else Slovo.Card,
                         style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (result == null) {
            MishaButton("SUBMIT", Modifier.fillMaxWidth()) {
                result = AnswerChecker.check(text, question.card.english)
            }
        } else {
            MishaButton("CONTINUE →", Modifier.fillMaxWidth()) {
                onContinue(result!!.verdict != AnswerChecker.Verdict.WRONG)
            }
        }
    }
}
