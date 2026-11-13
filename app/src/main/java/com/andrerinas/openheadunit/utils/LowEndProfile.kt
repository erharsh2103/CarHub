package com.andrerinas.openheadunit.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * One-time tuning for slow head units (2 GB RAM, 4 slow cores, 2.4 GHz-only Wi-Fi).
 *
 * Only defaults the user has never touched are changed, and only once: anything set by hand in
 * Settings afterwards wins. The Wi-Fi band stays on Auto, which already caps the stream on radios
 * without 5 GHz (see [Settings.narrowBandProfileCap]) and still uses 5 GHz where it exists.
 */
object LowEndProfile {

    private const val LOW_END_RAM_BYTES = 2_600L * 1024 * 1024

    fun isLowEndDevice(context: Context): Boolean = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val lowRamFlag = Build.VERSION.SDK_INT >= 19 && am.isLowRamDevice
        lowRamFlag || info.totalMem in 1..LOW_END_RAM_BYTES ||
            Runtime.getRuntime().availableProcessors() <= 2
    } catch (e: Exception) {
        false
    }

    fun applyOnce(context: Context, settings: Settings) {
        if (settings.lowEndProfileApplied) return
        settings.lowEndProfileApplied = true
        if (!settings.lowEndMode) return

        val changed = mutableListOf<String>()
        // AAC instead of raw PCM: ~1.5 Mbit/s of music becomes ~0.25 Mbit/s, which leaves room
        // on a crowded 2.4 GHz channel that the head unit's own Bluetooth also shares.
        if (!settings.hasStored("use-aac-audio")) { settings.useAacAudio = true; changed += "AAC media" }
        // 30 fps, not 60: the app's own measurements show 60 fps over a 2.4 GHz hotspot starves the
        // video link and the phone drops the session in a connect/disconnect loop, while 30 fps
        // holds. 30 fps also halves the bitrate, so less 2.4 GHz congestion and less re-buffering
        // (which is where a reconnect loop burns mobile data).
        if (!settings.hasStored("fps-limit")) { settings.fpsLimit = 30; changed += "30 fps" }
        // 720p, the app's own safe cap for a narrow-band link: sharp enough, and it survives 2.4 GHz
        // where 1080p does not. Drop to 480p by hand if the picture still stutters.
        if (!settings.hasStored("resolutionId")) {
            settings.resolutionId = Settings.Resolution._1280x720.id; changed += "720p"
        }
        AppLog.i("LowEndProfile: applied ${changed.ifEmpty { listOf("nothing (all set by hand)") }}")
    }
}
