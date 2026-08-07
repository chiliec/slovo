package cx.viz.slovo.domain

object StreakCalculator {
    fun next(previousStreak: Int, lastActiveEpochDay: Long, todayEpochDay: Long): Int = when {
        lastActiveEpochDay == 0L -> 1
        todayEpochDay == lastActiveEpochDay -> previousStreak
        todayEpochDay == lastActiveEpochDay + 1 -> previousStreak + 1
        else -> 1
    }

    /** True once a day has been fully skipped since [lastActiveEpochDay] — the streak would reset on next activity. */
    fun isBroken(streakDays: Int, lastActiveEpochDay: Long, todayEpochDay: Long): Boolean =
        streakDays > 0 && lastActiveEpochDay != 0L && todayEpochDay - lastActiveEpochDay >= 2
}
