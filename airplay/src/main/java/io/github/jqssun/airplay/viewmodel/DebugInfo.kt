package io.github.jqssun.airplay.viewmodel

// Debug/diagnostic data the AirPlay service and renderers publish. Copied from the upstream
// MainViewModel so CarHub can keep them without pulling in Compose/Hilt.

enum class LocalNetworkAccess { GRANTED, ASK, SETTINGS }

data class AudioDebug(
    val backlogMs: Int,
    val tunedCushionMs: Int,
    val trims: Int,
    val drops: Int,
    val silences: Int,
    val underruns: Int,
    val xrun: Int,
    val decodeMeanUs: Int,
    val decodeMaxUs: Int,
    val decodeHeld: Int,
    val decodeErrors: Int,
)

data class DebugInfo(
    val videoCodec: String = "",
    val videoRes: String = "",
    val videoFps: Int = 0,
    val videoBitrate: Long = 0,
    val videoFrames: Long = 0,
    val droppedFrames: Long = 0,
    val framePacingJitterUs: Long = 0,
    val audioCodec: String = "",
    val serverVolume: Int = 100,
    val clientVolume: Int = 100,
    val audio: AudioDebug? = null,
    val connections: Int = 0,
)
