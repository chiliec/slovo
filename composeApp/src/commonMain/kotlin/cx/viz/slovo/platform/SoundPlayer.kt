package cx.viz.slovo.platform

enum class Cue { SELECT, SUCCESS, ERROR }

interface SoundPlayer {
    suspend fun play(cue: Cue)
}

/** No cue assets bundled yet (Kosmo Phase 6 placeholder) — plumbing only. */
class NoopSoundPlayer : SoundPlayer {
    override suspend fun play(cue: Cue) { /* no-op */ }
}
