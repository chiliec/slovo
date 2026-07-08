package cx.viz.slovo.platform

import java.time.LocalDate

actual fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
