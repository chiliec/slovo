package cx.viz.slovo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import slovo.composeapp.generated.resources.ArchivoBlack_Regular
import slovo.composeapp.generated.resources.Res
import slovo.composeapp.generated.resources.SpaceGrotesk_Bold
import slovo.composeapp.generated.resources.SpaceGrotesk_Medium
import slovo.composeapp.generated.resources.SpaceGrotesk_Regular

@Composable fun displayFamily() = FontFamily(Font(Res.font.ArchivoBlack_Regular))

@Composable fun bodyFamily() = FontFamily(
    Font(Res.font.SpaceGrotesk_Regular, FontWeight.Normal),
    Font(Res.font.SpaceGrotesk_Medium, FontWeight.Medium),
    Font(Res.font.SpaceGrotesk_Bold, FontWeight.Bold),
)

@Composable
fun slovoTypography(): Typography {
    val display = displayFamily()
    val body = bodyFamily()
    return Typography(
        headlineLarge = TextStyle(fontFamily = display, fontSize = 26.sp),
        headlineMedium = TextStyle(fontFamily = display, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 15.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 13.sp),
        labelSmall = TextStyle(fontFamily = display, fontSize = 10.sp),
    )
}
