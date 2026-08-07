package cx.viz.slovo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import slovo.composeapp.generated.resources.Nunito_Bold
import slovo.composeapp.generated.resources.Nunito_ExtraBold
import slovo.composeapp.generated.resources.Nunito_Medium
import slovo.composeapp.generated.resources.Nunito_SemiBold
import slovo.composeapp.generated.resources.Res

@Composable fun nunitoFamily() = FontFamily(
    Font(Res.font.Nunito_Medium, FontWeight.Medium),
    Font(Res.font.Nunito_SemiBold, FontWeight.SemiBold),
    Font(Res.font.Nunito_Bold, FontWeight.Bold),
    Font(Res.font.Nunito_ExtraBold, FontWeight.ExtraBold),
)

@Composable
fun slovoTypography(): Typography {
    val nunito = nunitoFamily()
    return Typography(
        // display — screen headlines ("LESSON DONE", "SLOVO")
        headlineLarge = TextStyle(fontFamily = nunito, fontWeight = FontWeight.ExtraBold, fontSize = 37.sp),
        // title — card titles, big stat numbers
        headlineMedium = TextStyle(fontFamily = nunito, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
        // card title — prompts, subtitles
        titleMedium = TextStyle(fontFamily = nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp),
        // body — secondary text
        bodyMedium = TextStyle(fontFamily = nunito, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        // button — CAPS labels, chips, nav tabs
        labelSmall = TextStyle(
            fontFamily = nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.5.sp,
        ),
    )
}
