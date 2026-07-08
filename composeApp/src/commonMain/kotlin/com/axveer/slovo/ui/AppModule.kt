package com.axveer.slovo.ui

import com.axveer.slovo.data.ContentRepository
import com.axveer.slovo.data.ProgressRepository
import com.axveer.slovo.platform.AudioPlayer

class AppModule(
    val content: ContentRepository,
    val progress: ProgressRepository,
    val audio: AudioPlayer,
)
