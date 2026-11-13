package io.github.jqssun.airplay

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import io.github.jqssun.airplay.service.AirPlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * CarHub's AirPlay screen (replaces the upstream Compose UI to keep the app small on 2 GB units).
 * Shows the iPhone's mirrored screen, AirPlay video, or the playing song, and keeps the server on.
 */
class MainActivity : Activity() {

    private var service: AirPlayService? = null
    private var scope: CoroutineScope? = null
    private lateinit var mirror: SurfaceView
    private lateinit var playback: SurfaceView
    private lateinit var status: TextView
    private var aspect = 16f / 9f

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as? AirPlayService.LocalBinder)?.service ?: return
            service = svc
            if (svc.serverState.value != AirPlayService.ServerState.RUNNING) svc.startServer()
            mirror.holder.surface?.takeIf { it.isValid }?.let(svc::setVideoSurface)
            playback.holder.surface?.takeIf { it.isValid }?.let(svc::setVideoPlaybackSurface)
            observe(svc)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        mirror = surface { setVideoSurface(it) } and { clearVideoSurface(it) }
        playback = surface { setVideoPlaybackSurface(it) } and { clearVideoPlaybackSurface(it) }
        playback.visibility = View.GONE
        status = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        root.addView(mirror, centered())
        root.addView(playback, centered())
        root.addView(status, FrameLayout.LayoutParams(-1, -1))
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fit() }
        setContentView(root)

        bindService(Intent(this, AirPlayService::class.java), connection, BIND_AUTO_CREATE)
    }

    private fun observe(svc: AirPlayService) {
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { s ->
            s.launch {
                combine(svc.serverState, svc.mirroringActive, svc.videoPlaybackActive, svc.audioOnly, svc.trackInfo) {
                    state, mirroring, video, audio, track -> listOf(state, mirroring, video, audio, track)
                }.collect { v ->
                    val state = v[0] as AirPlayService.ServerState
                    val mirroring = v[1] as Boolean
                    val video = v[2] as Boolean
                    val audio = v[3] as Boolean
                    val track = v[4] as io.github.jqssun.airplay.audio.TrackInfo
                    playback.visibility = if (video) View.VISIBLE else View.GONE
                    mirror.visibility = if (video) View.GONE else View.VISIBLE
                    status.visibility = if (mirroring || video) View.GONE else View.VISIBLE
                    status.text = when {
                        state == AirPlayService.ServerState.ERROR -> "AirPlay could not start.\nCheck that the stereo's Wi-Fi hotspot is on."
                        state != AirPlayService.ServerState.RUNNING -> "Starting AirPlay…"
                        audio && track.title.isNotEmpty() -> "♪  ${track.title}\n${track.artist}"
                        audio -> "♪  Playing from iPhone"
                        else -> "AirPlay ready: \"${svc.serverName()}\"\n\n" +
                            "1. On the iPhone, join this stereo's Wi-Fi hotspot.\n" +
                            "2. Music: tap the AirPlay icon and pick \"${svc.serverName()}\".\n" +
                            "3. Screen: Control Centre → Screen Mirroring → \"${svc.serverName()}\"."
                    }
                }
            }
            s.launch { svc.videoAspect.collect { aspect = it; fit() } }
        }
    }

    private fun AirPlayService.serverName(): String =
        getSharedPreferences(Prefs.NAME, MODE_PRIVATE)[Prefs.SERVER_NAME].ifBlank { Prefs.SERVER_NAME.def }

    /** Letterbox the mirrored picture to the iPhone's aspect ratio inside the stereo's panel. */
    private fun fit() {
        val root = mirror.parent as? View ?: return
        val w = root.width; val h = root.height
        if (w == 0 || h == 0) return
        val (vw, vh) = if (w.toFloat() / h > aspect) ((h * aspect).toInt() to h) else (w to (w / aspect).toInt())
        val lp = mirror.layoutParams
        if (lp.width != vw || lp.height != vh) {
            lp.width = vw; lp.height = vh
            mirror.layoutParams = lp
        }
    }

    private fun centered() = FrameLayout.LayoutParams(-1, -1, Gravity.CENTER)

    private class SurfaceBinding(val view: SurfaceView, val onAvailable: AirPlayService.(Surface) -> Unit)

    private fun surface(onAvailable: AirPlayService.(Surface) -> Unit) = SurfaceBinding(SurfaceView(this), onAvailable)

    private infix fun SurfaceBinding.and(onGone: AirPlayService.(Surface) -> Unit): SurfaceView {
        val binding = this
        view.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) { service?.let { binding.onAvailable(it, holder.surface) } }
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) { service?.let { binding.onAvailable(it, holder.surface) } }
            override fun surfaceDestroyed(holder: SurfaceHolder) { service?.let { onGone(it, holder.surface) } }
        })
        return view
    }

    override fun onDestroy() {
        scope?.cancel()
        try { unbindService(connection) } catch (e: IllegalArgumentException) { }
        super.onDestroy()
    }
}
