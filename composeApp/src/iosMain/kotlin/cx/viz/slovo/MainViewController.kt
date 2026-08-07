package cx.viz.slovo

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import cx.viz.slovo.data.BundledContentRepository
import cx.viz.slovo.data.DriverFactory
import cx.viz.slovo.data.ProgressRepository
import cx.viz.slovo.db.SlovoDatabase
import cx.viz.slovo.platform.IosAudioPlayer
import cx.viz.slovo.platform.IosHaptics
import cx.viz.slovo.platform.NoopSoundPlayer
import cx.viz.slovo.ui.App
import cx.viz.slovo.ui.AppModule

fun MainViewController() = ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
    val db = remember { SlovoDatabase(DriverFactory().createDriver()) }
    val module = remember {
        AppModule(
            content = BundledContentRepository(),
            progress = ProgressRepository(db),
            audio = IosAudioPlayer(),
            sound = NoopSoundPlayer(),
            haptics = IosHaptics(),
        )
    }
    App(module)
}
