package cx.viz.slovo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SlovoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Slovo.Red,
            background = Slovo.Sand,
            surface = Slovo.Card,
            onPrimary = Slovo.Card,
            onBackground = Slovo.Ink,
            onSurface = Slovo.Ink,
        ),
        typography = slovoTypography(),
        content = content,
    )
}
