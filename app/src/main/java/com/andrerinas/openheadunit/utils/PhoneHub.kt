package com.andrerinas.openheadunit.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat

/**
 * One app for both kinds of phone. Android phones stay here for Android Auto; when an iPhone plugs
 * in over USB or joins over Bluetooth, CarHub opens its built-in AirPlay screen so the iPhone's
 * music and mirrored screen play on the stereo.
 *
 * AirPlay runs entirely over the stereo's Wi-Fi hotspot, so it does not need the Bluetooth data
 * channel that wireless CarPlay requires and that the maker's own Bluetooth module cannot provide.
 * A stereo that does have a usable CarPlay path (a USB dongle, or built-in CarPlay) can be pointed
 * at an installed CarPlay app instead by setting [Settings.iphoneHandoffPackage].
 */
object PhoneHub {

    const val DEFAULT_CARPLAY_PACKAGE = "com.shihab.diplay"
    /** Built-in AirPlay, the default: not a package, handled in-process. */
    const val AIRPLAY = "airplay"
    private const val APPLE_USB_VENDOR_ID = 0x05AC
    private const val COOLDOWN_MS = 30_000L
    private const val AIRPLAY_ACTIVITY = "io.github.jqssun.airplay.MainActivity"

    @Volatile private var lastHandoffMs = -COOLDOWN_MS
    @Volatile private var registered = false

    /** Live USB attaches only reach runtime receivers; Bluetooth also arrives via [Receiver]. */
    fun register(context: Context, settings: Settings) {
        if (registered) return
        registered = true
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        ContextCompat.registerReceiver(context.applicationContext, Receiver(), filter,
            ContextCompat.RECEIVER_EXPORTED)
        AppLog.i("PhoneHub: iPhone hand-off ${if (settings.iphoneHandoffEnabled) "on" else "off"} -> ${settings.iphoneHandoffPackage}")
    }

    @SuppressLint("MissingPermission")
    internal fun isIphone(intent: Intent): Boolean = when (intent.action) {
        UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            device?.vendorId == APPLE_USB_VENDOR_ID
        }
        BluetoothDevice.ACTION_ACL_CONNECTED -> {
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val name = try { device?.name } catch (e: SecurityException) { null }
            name != null && looksLikeIphone(name)
        }
        else -> false
    }

    /** Default iPhone names are "iPhone", "<Name>'s iPhone" or "iPhone 15 Pro". */
    internal fun looksLikeIphone(name: String): Boolean = name.contains("iphone", ignoreCase = true)

    /**
     * Open the iPhone connection now because the user asked (the manual icon / tile). Ignores the
     * auto-hand-off switch and the cooldown: a deliberate tap should always do something.
     */
    fun handOffManual(context: Context) {
        lastHandoffMs = -COOLDOWN_MS
        val target = Settings(context).iphoneHandoffPackage
        if (target == AIRPLAY || target.isBlank()) openAirPlay(context)
        else context.packageManager.getLaunchIntentForPackage(target)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try { context.startActivity(it) } catch (e: Exception) { AppLog.w("PhoneHub: manual open of $target failed", e) }
        } ?: openAirPlay(context)
    }

    fun handOff(context: Context, reason: String) {
        val settings = Settings(context)
        if (!settings.iphoneHandoffEnabled) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastHandoffMs < COOLDOWN_MS) return

        val target = settings.iphoneHandoffPackage
        if (target == AIRPLAY || target.isBlank()) {
            if (openAirPlay(context)) lastHandoffMs = now
            else AppLog.w("PhoneHub: iPhone seen ($reason) but the AirPlay screen could not open")
            return
        }
        val launch = context.packageManager.getLaunchIntentForPackage(target)
        if (launch == null) {
            AppLog.w("PhoneHub: iPhone seen ($reason) but $target is not installed")
            return
        }
        lastHandoffMs = now
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            context.startActivity(launch)
            AppLog.i("PhoneHub: iPhone ($reason) handed to $target")
        } catch (e: Exception) {
            AppLog.w("PhoneHub: could not open $target", e)
        }
    }

    /** AirPlay needs Android 6.0; its native code is 32/64-bit ARM only. */
    private fun openAirPlay(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            AppLog.w("PhoneHub: AirPlay needs Android 6.0 or newer")
            return false
        }
        return try {
            val intent = Intent().setClassName(context.packageName, AIRPLAY_ACTIVITY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
            AppLog.i("PhoneHub: iPhone handed to built-in AirPlay")
            true
        } catch (e: Exception) {
            AppLog.w("PhoneHub: could not open AirPlay", e)
            false
        }
    }

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (isIphone(intent)) {
                    val via = if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) "USB" else "Bluetooth"
                    handOff(context, via)
                }
            } catch (e: Exception) {
                // Settings are unreadable before the first unlock; an iPhone then waits for the user.
                AppLog.w("PhoneHub: hand-off skipped", e)
            }
        }
    }
}
