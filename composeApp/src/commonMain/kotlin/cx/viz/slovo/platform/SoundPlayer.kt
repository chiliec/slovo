package cx.viz.slovo.platform

enum class Cue(val fileName: String) {
    SELECT("cue-select.m4a"),
    SUCCESS("cue-success.m4a"),
    ERROR("cue-error.m4a"),
    PAIR_MATCH("cue-pair-match.m4a"),
    LESSON_COMPLETE("cue-lesson-complete.m4a"),
    STREAK_RESCUED("cue-streak-rescued.m4a"),
}

interface SoundPlayer {
    suspend fun play(cue: Cue)
}

class NoopSoundPlayer : SoundPlayer {
    override suspend fun play(cue: Cue) { /* no-op */ }
}
