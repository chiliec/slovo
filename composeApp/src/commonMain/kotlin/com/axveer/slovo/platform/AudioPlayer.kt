package com.axveer.slovo.platform

interface AudioPlayer {
    suspend fun play(fileName: String)
}

class NoopAudioPlayer : AudioPlayer {
    override suspend fun play(fileName: String) { /* no-op */ }
}
