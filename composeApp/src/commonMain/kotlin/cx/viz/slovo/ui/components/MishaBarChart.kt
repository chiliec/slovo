package cx.viz.slovo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cx.viz.slovo.ui.theme.Slovo

/** One bar in a [MishaBarChart]: an axis [label] and its [value]. */
data class Bar(val label: String, val value: Int)

/**
 * A neo-brutalist mini bar chart: hard-bordered filled bars, one per [bars] entry,
 * bar height proportional to value / max. Content-agnostic (no SRS knowledge).
 * A zero-value bar still renders a thin baseline stub so the axis stays legible.
 */
@Composable
fun MishaBarChart(
    bars: List<Bar>,
    barColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
) {
    val maxValue = maxOf(1, bars.maxOfOrNull { it.value } ?: 1)
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    bar.value.toString(),
                    color = Slovo.Ink,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(maxOf(3.dp, height * (bar.value.toFloat() / maxValue)))
                        .background(barColor)
                        .border(2.dp, Slovo.Ink),
                )
                Text(
                    bar.label,
                    color = Slovo.Ink.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
