package com.axveer.slovo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.axveer.slovo.data.ContentRepository
import com.axveer.slovo.data.DriverFactory
import com.axveer.slovo.data.ProgressRepository
import com.axveer.slovo.db.SlovoDatabase
import com.axveer.slovo.platform.AndroidAudioPlayer
import com.axveer.slovo.ui.App
import com.axveer.slovo.ui.AppModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = SlovoDatabase(DriverFactory(applicationContext).createDriver())
        val module = AppModule(
            content = ContentRepository(),
            progress = ProgressRepository(db),
            audio = AndroidAudioPlayer(applicationContext),
        )
        setContent { App(module) }
    }
}
