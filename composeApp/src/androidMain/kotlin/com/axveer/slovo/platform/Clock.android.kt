package com.axveer.slovo.platform

import java.time.LocalDate

actual fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
