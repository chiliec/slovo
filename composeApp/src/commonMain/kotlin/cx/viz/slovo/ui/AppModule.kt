package cx.viz.slovo.ui

import cx.viz.slovo.data.ContentRepository
import cx.viz.slovo.data.ProgressRepository
import cx.viz.slovo.platform.AudioPlayer

class AppModule(
    val content: ContentRepository,
    val progress: ProgressRepository,
    val audio: AudioPlayer,
)
