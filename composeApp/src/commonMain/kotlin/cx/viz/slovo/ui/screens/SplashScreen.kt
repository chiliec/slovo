package cx.viz.slovo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import cx.viz.slovo.ui.components.MishaMascot
import cx.viz.slovo.ui.theme.Slovo
import kotlinx.coroutines.delay

/** Cold-start splash: floating mascot + wordmark over twinkling stars, blue moment-screen backdrop. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        onFinished()
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Slovo.Blue)) {
        val starSpots = remember { listOf(0.18f to 0.22f, 0.78f to 0.16f, 0.14f to 0.72f, 0.85f to 0.68f, 0.5f to 0.12f) }
        starSpots.forEachIndexed { i, (fx, fy) ->
            TwinklingStar(Modifier.offset(x = maxWidth * fx, y = maxHeight * fy), staggerMs = i * 220)
        }
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FloatingMascot()
            Text("SLOVO ◆", color = Slovo.Card, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
private fun FloatingMascot() {
    val transition = rememberInfiniteTransition()
    val y by transition.animateFloat(0f, -7f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse))
    MishaMascot(size = 96.dp, modifier = Modifier.offset(y = y.dp))
}

@Composable
private fun TwinklingStar(modifier: Modifier, staggerMs: Int) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(1800, delayMillis = staggerMs, easing = LinearEasing), RepeatMode.Reverse),
    )
    val scale by transition.animateFloat(
        0.8f, 1.2f, infiniteRepeatable(tween(1800, delayMillis = staggerMs, easing = LinearEasing), RepeatMode.Reverse),
    )
    Canvas(modifier.size(10.dp)) {
        scale(scale) {
            rotate(45f) {
                drawRect(Color.White.copy(alpha = alpha), size = size)
            }
        }
    }
}
