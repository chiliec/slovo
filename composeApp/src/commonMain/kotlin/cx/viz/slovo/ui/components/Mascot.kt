package cx.viz.slovo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import cx.viz.slovo.ui.theme.Slovo

/** Idle-pose cosmonaut, geometry lifted from the Kosmo design's inline SVG (KosmoFlow.dc.html). */
@Composable
fun MishaMascot(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        fun p(v: Float) = v / 100f * s
        fun ink(width: Float) = Stroke(width = p(width))

        // antenna
        drawLine(Slovo.Ink, Offset(p(50f), p(10f)), Offset(p(50f), p(4f)), strokeWidth = p(2.5f))
        drawCircle(Slovo.Red, radius = p(3.5f), center = Offset(p(50f), p(4f)))
        drawCircle(Slovo.Ink, radius = p(3.5f), center = Offset(p(50f), p(4f)), style = ink(2f))

        // helmet ring
        drawCircle(Color.White.copy(alpha = 0.22f), radius = p(38f), center = Offset(p(50f), p(48f)))
        drawCircle(Slovo.Ink, radius = p(38f), center = Offset(p(50f), p(48f)), style = ink(2.5f))

        // side fins
        fun fin(cx: Float, cy: Float, rotationDeg: Float) {
            val topLeft = Offset(p(cx) - p(8f), p(cy) - p(12f))
            val fSize = Size(p(16f), p(24f))
            rotate(rotationDeg, pivot = Offset(p(cx), p(cy))) {
                drawOval(Slovo.MascotDark, topLeft, fSize)
                drawOval(Slovo.Ink, topLeft, fSize, style = ink(2.5f))
            }
        }
        fin(31f, 33f, -28f)
        fin(69f, 33f, 28f)

        // head
        drawCircle(Slovo.Mascot, radius = p(25f), center = Offset(p(50f), p(50f)))
        drawCircle(Slovo.Ink, radius = p(25f), center = Offset(p(50f), p(50f)), style = ink(2.5f))

        // visor
        val visorTopLeft = Offset(p(50f) - p(11f), p(58f) - p(8f))
        val visorSize = Size(p(22f), p(16f))
        drawOval(Slovo.MascotLight, visorTopLeft, visorSize)
        drawOval(Slovo.Ink, visorTopLeft, visorSize, style = ink(2f))

        // eyes + nose
        drawCircle(Slovo.Ink, radius = p(3f), center = Offset(p(41f), p(45f)))
        drawCircle(Slovo.Ink, radius = p(3f), center = Offset(p(59f), p(45f)))
        drawCircle(Slovo.Ink, radius = p(3.5f), center = Offset(p(50f), p(55f)))

        // mouth
        val mouth = Path().apply {
            moveTo(p(44f), p(62f))
            quadraticBezierTo(p(50f), p(66f), p(56f), p(62f))
        }
        drawPath(mouth, Slovo.Ink, style = Stroke(width = p(2f), cap = StrokeCap.Round))
    }
}
