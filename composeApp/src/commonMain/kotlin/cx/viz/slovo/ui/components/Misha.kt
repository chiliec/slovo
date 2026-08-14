package cx.viz.slovo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cx.viz.slovo.domain.Hearts
import cx.viz.slovo.ui.theme.Slovo

/**
 * The MISHA hard-offset shadow: a solid ink rectangle sized to *exactly* match the
 * surface and placed [dx]/[dy] behind it. Uses a custom layout (rather than a Box +
 * matchParentSize) so the shadow tracks the surface's measured size, not the parent.
 * That keeps a full-width card/button (surface stretched by a fillMaxWidth/weight
 * modifier) from showing the ink shadow poking out beside a content-width surface.
 */
@Composable
private fun HardShadow(modifier: Modifier, dx: Dp, dy: Dp, surface: @Composable () -> Unit) {
    Layout(
        modifier = modifier,
        contents = listOf({ Box(Modifier.background(Slovo.Ink)) }, surface),
    ) { measurables, constraints ->
        val s = measurables[1].first().measure(constraints)
        val sh = measurables[0].first().measure(Constraints.fixed(s.width, s.height))
        val ox = dx.roundToPx(); val oy = dy.roundToPx()
        layout(s.width, s.height) {
            sh.place(ox, oy)
            s.place(0, 0)
        }
    }
}

@Composable
fun MishaCard(
    modifier: Modifier = Modifier,
    shadow: Dp = 3.dp,
    background: Color = Slovo.Card,
    content: @Composable BoxScope.() -> Unit,
) {
    HardShadow(modifier, shadow, shadow) {
        Box(Modifier.background(background).border(2.5.dp, Slovo.Ink).padding(1.dp), content = content)
    }
}

@Composable
fun MishaButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Slovo.Red,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val offset by animateDpAsState(if (pressed) 2.dp else 0.dp)
    HardShadow(modifier, 4.dp, 4.dp) {
        Box(
            Modifier
                .offset(offset, offset)
                .background(if (enabled) background else background.copy(alpha = 0.4f))
                .border(2.5.dp, Slovo.Ink)
                .let {
                    if (enabled) it.clickable { pressed = true; onClick(); pressed = false } else it
                }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Card/Yellow backgrounds would swallow a near-white label ("← BACK" rendered blank).
            Text(text, color = if (background.luminance() > 0.5f) Slovo.Ink else Slovo.Card,
                 style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MishaStatChip(
    value: String,
    label: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    MishaCard(modifier, shadow = 4.dp, background = background) {
        Column(Modifier.padding(10.dp)) {
            Text(value, color = textColor, style = MaterialTheme.typography.headlineMedium)
            Text(label, color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun HeartsRow(hearts: Int, modifier: Modifier = Modifier, max: Int = Hearts.MAX) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { i ->
            Text("♥", color = Slovo.Red.copy(alpha = if (i < hearts) 1f else 0.18f),
                 style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Shared quiz-step header: position + hearts on top, an animated progress bar below. */
@Composable
fun QuizHeader(index: Int, total: Int, hearts: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        // end padding keeps the last heart clear of the ✕ quit button overlaid at the top-right.
        Row(Modifier.fillMaxWidth().padding(end = 32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1} / $total", color = Slovo.Ink, style = MaterialTheme.typography.bodyMedium)
            HeartsRow(hearts)
        }
        Spacer(Modifier.height(6.dp))
        val fraction by animateFloatAsState(if (total == 0) 0f else (index + 1f) / total, animationSpec = tween(400))
        Box(Modifier.fillMaxWidth().height(6.dp).background(Slovo.Card).border(1.dp, Slovo.Ink)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Slovo.Blue))
        }
    }
}

@Composable
fun MishaTicker(text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val shift by transition.animateFloat(
        0f, -1000f, infiniteRepeatable(tween(14000, easing = LinearEasing))
    )
    Box(
        modifier
            .fillMaxWidth()
            .background(Slovo.Yellow)
            .border(width = 2.5.dp, color = Slovo.Ink)
            .clipToBounds()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text,
            modifier = Modifier.offset(x = shift.dp),
            maxLines = 1,
            color = Slovo.Ink,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun LessonRow(
    index: Int, title: String, subtitle: String,
    done: Boolean, current: Boolean, locked: Boolean, onClick: () -> Unit,
) {
    val bg = when { current -> Slovo.Ink; else -> Slovo.Card }
    val fg = if (current) Slovo.Card else Slovo.Ink
    MishaCard(
        modifier = Modifier.fillMaxWidth()
            .let { if (locked) it else it.clickable { onClick() } },
        shadow = 3.dp, background = if (locked) Slovo.Sand else bg,
    ) {
        Row(Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.background(Slovo.Yellow)
                .border(2.dp, Slovo.Ink).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(index.toString().padStart(2, '0'), color = Slovo.Ink,
                     style = MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = if (locked) fg.copy(alpha = 0.5f) else fg,
                     style = MaterialTheme.typography.titleMedium)
                Text(if (locked) "LOCKED" else subtitle, color = fg.copy(alpha = 0.55f),
                     style = MaterialTheme.typography.bodyMedium)
            }
            Text(when { done -> "DONE"; current -> "GO →"; locked -> "🔒"; else -> "" },
                 color = if (current) Slovo.Yellow else Slovo.Red,
                 style = MaterialTheme.typography.labelSmall)
        }
    }
}
