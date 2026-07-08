package com.axveer.slovo

import androidx.compose.ui.window.ComposeUIViewController
import com.axveer.slovo.data.ContentRepository
import com.axveer.slovo.data.DriverFactory
import com.axveer.slovo.data.ProgressRepository
import com.axveer.slovo.db.SlovoDatabase
import com.axveer.slovo.platform.IosAudioPlayer
import com.axveer.slovo.ui.App
import com.axveer.slovo.ui.AppModule

fun MainViewController() = ComposeUIViewController {
    val db = SlovoDatabase(DriverFactory().createDriver())
    val module = AppModule(
        content = ContentRepository(),
        progress = ProgressRepository(db),
        audio = IosAudioPlayer(),
    )
    App(module)
}
