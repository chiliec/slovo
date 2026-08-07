package cx.viz.slovo.ui

import cx.viz.slovo.data.ContentRepository
import cx.viz.slovo.data.ProgressRepository
import cx.viz.slovo.platform.AudioPlayer
import cx.viz.slovo.platform.Cue
import cx.viz.slovo.platform.Haptics
import cx.viz.slovo.platform.SoundPlayer

class AppModule(
    val content: ContentRepository,
    val progress: ProgressRepository,
    val audio: AudioPlayer,
    val sound: SoundPlayer,
    val haptics: Haptics,
)

suspend fun AppModule.playCue(cue: Cue) {
    if (progress.settings().soundsEnabled) sound.play(cue)
}
