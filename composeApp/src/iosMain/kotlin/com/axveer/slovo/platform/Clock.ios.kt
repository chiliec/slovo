package com.axveer.slovo.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
actual fun todayEpochDay(): Long {
    // Seconds since epoch → whole local days. NSDate is UTC; adjust by the local offset.
    val now = NSDate()
    val tzOffset = NSCalendar.currentCalendar.timeZone.secondsFromGMTForDate(now)
    val localSeconds = now.timeIntervalSince1970 + tzOffset.toDouble()
    return (localSeconds / 86_400.0).toLong()
}
