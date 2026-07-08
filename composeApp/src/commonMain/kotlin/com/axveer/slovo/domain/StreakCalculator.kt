package com.axveer.slovo.domain

object StreakCalculator {
    fun next(previousStreak: Int, lastActiveEpochDay: Long, todayEpochDay: Long): Int = when {
        lastActiveEpochDay == 0L -> 1
        todayEpochDay == lastActiveEpochDay -> previousStreak
        todayEpochDay == lastActiveEpochDay + 1 -> previousStreak + 1
        else -> 1
    }
}
