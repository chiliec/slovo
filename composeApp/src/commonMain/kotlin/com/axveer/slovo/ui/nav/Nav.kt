package com.axveer.slovo.ui.nav

sealed class Dest(val route: String) {
    data object Learn : Dest("learn")
    data object Drill : Dest("drill")
    data object League : Dest("league")
    data object You : Dest("you")
    data object Lesson : Dest("lesson/{unitId}/{lessonId}") {
        fun of(unitId: String, lessonId: String) = "lesson/$unitId/$lessonId"
    }
}

val bottomTabs = listOf(Dest.Learn to "LEARN", Dest.Drill to "DRILL", Dest.League to "LEAGUE", Dest.You to "YOU")
