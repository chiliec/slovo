package com.axveer.slovo.platform

import android.content.Context

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    override suspend fun play(fileName: String) { /* real impl in Task 12 */ }
}
