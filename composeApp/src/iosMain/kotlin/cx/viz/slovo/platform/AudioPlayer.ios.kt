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
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosAudioPlayer : AudioPlayer {
    private var player: AVAudioPlayer? = null
    private val mutex = Mutex()

    override suspend fun play(fileName: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            try {
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
