package com.axveer.slovo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axveer.slovo.ui.theme.Slovo

@Composable
fun MishaCard(
    modifier: Modifier = Modifier,
    shadow: Dp = 4.dp,
    background: Color = Slovo.Card,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        // hard ink shadow: a solid offset rectangle behind the surface
        Box(
            Modifier.matchParentSize().offset(shadow, shadow).background(Slovo.Ink)
        )
        Box(
            Modifier.background(background).border(3.dp, Slovo.Ink).padding(1.dp),
            content = content,
        )
    }
}

@Composable
fun MishaButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Slovo.Red,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val offset by animateDpAsState(if (pressed) 2.dp else 0.dp)
    Box(modifier) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).background(Slovo.Ink))
        Box(
            Modifier
                .offset(offset, offset)
                .background(background)
                .border(3.dp, Slovo.Ink)
                .clickable {
                    pressed = true
                    onClick()
                    pressed = false
                }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Slovo.Card, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
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
            Text(value, color = textColor, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Text(label, color = textColor.copy(alpha = 0.7f), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
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
            .border(width = 3.dp, color = Slovo.Ink)
            .clipToBounds()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text,
            modifier = Modifier.offset(x = shift.dp),
            maxLines = 1,
            color = Slovo.Ink,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        )
    }
}
