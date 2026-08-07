package cx.viz.slovo.platform

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import slovo.composeapp.generated.resources.Res
import java.io.File

class AndroidSoundPlayer(private val context: Context) : SoundPlayer {
    private var player: MediaPlayer? = null
    private val mutex = Mutex()

    override suspend fun play(cue: Cue) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val bytes = Res.readBytes("files/audio/${cue.fileName}")
                val tmp = File(context.cacheDir, cue.fileName).apply { writeBytes(bytes) }
                player?.release(); player = null
                val mp = MediaPlayer()
                mp.setDataSource(tmp.absolutePath)
                mp.setOnCompletionListener { it.release() }
                mp.prepare(); mp.start()
                player = mp
            } catch (e: Exception) {
                // a cue sound is an enhancement, never a blocker — swallow
            }
        }
        Unit
    }
}
