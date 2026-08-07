package cx.viz.slovo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cx.viz.slovo.data.BundledContentRepository
import cx.viz.slovo.data.DriverFactory
import cx.viz.slovo.data.ProgressRepository
import cx.viz.slovo.db.SlovoDatabase
import cx.viz.slovo.platform.AndroidAudioPlayer
import cx.viz.slovo.platform.AndroidHaptics
import cx.viz.slovo.platform.NoopSoundPlayer
import cx.viz.slovo.ui.App
import cx.viz.slovo.ui.AppModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = SlovoDatabase(DriverFactory(applicationContext).createDriver())
        val module = AppModule(
            content = BundledContentRepository(),
            progress = ProgressRepository(db),
            audio = AndroidAudioPlayer(applicationContext),
            sound = NoopSoundPlayer(),
            haptics = AndroidHaptics(applicationContext),
        )
        setContent { App(module) }
    }
}
