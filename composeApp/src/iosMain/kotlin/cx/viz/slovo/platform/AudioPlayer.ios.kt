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
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosAudioPlayer : AudioPlayer {
    private var player: AVAudioPlayer? = null
    private val mutex = Mutex()

    /**
     * The default session category (.soloAmbient) is muted by the iPhone ringer
     * switch, so clips are silent on a silenced device. Simulators have no such
     * switch, which is why this never reproduced there. Activate .playback so
     * audio is audible regardless of the switch.
     *
     * Re-set on every play (not cached) because IosSoundPlayer shares this same
     * process-wide AVAudioSession and switches it to .ambient for UI cues, which
     * *should* honor the silent switch — a phrase clip must reclaim .playback
     * afterwards or it would go silent too.
     */
    private fun ensurePlaybackSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)
    }

    override suspend fun play(fileName: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            try {
                ensurePlaybackSession()
                val bytes = Res.readBytes("files/audio/$fileName")
                val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
                player = AVAudioPlayer(data = data, error = null)
                player?.prepareToPlay()
                player?.play()
            } catch (e: Exception) { /* swallow */ }
        }
        Unit
    }
}
