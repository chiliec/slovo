package cx.viz.slovo.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import slovo.composeapp.generated.resources.Res
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create

/** Unlike IosAudioPlayer, UI cues use .ambient so they respect the silent switch (see AudioPlayer.ios.kt). */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSoundPlayer : SoundPlayer {
    private var player: AVAudioPlayer? = null
    private val mutex = Mutex()

    override suspend fun play(cue: Cue) = withContext(Dispatchers.Default) {
        mutex.withLock {
            try {
                val session = AVAudioSession.sharedInstance()
                session.setCategory(AVAudioSessionCategoryAmbient, null)
                session.setActive(true, null)
                val bytes = Res.readBytes("files/audio/${cue.fileName}")
                val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
                player = AVAudioPlayer(data = data, error = null)
                player?.prepareToPlay()
                player?.play()
            } catch (e: Exception) { /* a cue sound is an enhancement, never a blocker — swallow */ }
        }
        Unit
    }
}
