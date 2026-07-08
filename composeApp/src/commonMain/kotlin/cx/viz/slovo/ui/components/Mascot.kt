package cx.viz.slovo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import cx.viz.slovo.ui.theme.Slovo

@Composable
fun MishaMascot(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        fun p(v: Float) = v / 100f * s
        val stroke = Stroke(width = p(4f))
        // ears
        drawCircle(Slovo.Bear, radius = p(11f), center = Offset(p(26f), p(24f)))
        drawCircle(Slovo.Ink, radius = p(11f), center = Offset(p(26f), p(24f)), style = stroke)
        drawCircle(Slovo.Bear, radius = p(11f), center = Offset(p(74f), p(24f)))
        drawCircle(Slovo.Ink, radius = p(11f), center = Offset(p(74f), p(24f)), style = stroke)
        // head
        val head = Offset(p(18f), p(18f)); val headSize = Size(p(64f), p(64f)); val r = CornerRadius(p(18f), p(18f))
        drawRoundRect(Slovo.Bear, head, headSize, r)
        drawRoundRect(Slovo.Ink, head, headSize, r, style = stroke)
        // muzzle
        val mz = Offset(p(36f), p(52f)); val mzSize = Size(p(28f), p(20f)); val mr = CornerRadius(p(10f), p(10f))
        drawRoundRect(Slovo.BearLt, mz, mzSize, mr)
        drawRoundRect(Slovo.Ink, mz, mzSize, mr, style = Stroke(width = p(3.5f)))
        // eyes + nose
        drawRect(Slovo.Ink, Offset(p(33f), p(38f)), Size(p(8f), p(8f)))
        drawRect(Slovo.Ink, Offset(p(59f), p(38f)), Size(p(8f), p(8f)))
        drawRoundRect(Slovo.Ink, Offset(p(45f), p(54f)), Size(p(10f), p(8f)), CornerRadius(p(2f), p(2f)))
    }
}
