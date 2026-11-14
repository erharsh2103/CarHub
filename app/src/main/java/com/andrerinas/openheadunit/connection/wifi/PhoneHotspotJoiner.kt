package com.andrerinas.openheadunit.connection.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import com.andrerinas.openheadunit.connection.wifi.modes.nativeaa.SoftApState
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.HotspotManager
import com.andrerinas.openheadunit.utils.Settings
import com.andrerinas.openheadunit.utils.SoftApStateReader

/**
 * Phone hotspot client mode: join this unit's Wi-Fi to the phone's hotspot, so the normal
 * Headunit Server discovery (which already dials the Wi-Fi gateway on port 5277) finds Android
 * Auto's head unit server on the phone.
 *
 * Why this topology: when the phone joins the *unit's* hotspot, the phone sees a network with no
 * internet and tends to drop or deprioritise it, which shows up as reconnect loops and mobile-data
 * churn. With the phone as the hotspot, the phone keeps its mobile data and the link is stable.
 * It also needs no Bluetooth, which units whose Bluetooth is the maker's own module cannot offer.
 *
 * Blocking (waits for the unit's own AP to go down and the join to land), so never call it on the
 * main thread.
 */
object PhoneHotspotJoiner {

    private const val JOIN_WAIT_MS = 15_000L
    private const val POLL_MS = 500L

    /** True once the unit is on the phone's hotspot (or nothing was configured to join). */
    fun ensureJoined(context: Context, settings: Settings): Boolean {
        if (!settings.joinsPhoneHotspot) return true
        val ssid = settings.phoneHotspotSsid.trim()
        val password = settings.phoneHotspotPassword
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return false

        if (isOn(wifi, ssid)) {
            AppLog.i("PhoneHotspot: already on \"$ssid\"")
            return true
        }

        // Most head units cannot be an access point and a Wi-Fi client at once.
        if (SoftApStateReader.read(context) == SoftApState.ENABLED) {
            AppLog.i("PhoneHotspot: turning this unit's own hotspot off to join \"$ssid\"")
            HotspotManager.disableAndAwaitDown(context)
        }

        @Suppress("DEPRECATION")
        if (!wifi.isWifiEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) wifi.isWifiEnabled = true

        val asked = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) joinLegacy(wifi, ssid, password)
            else suggest(wifi, ssid, password)
        if (!asked) return false

        val deadline = System.currentTimeMillis() + JOIN_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            if (isOn(wifi, ssid)) {
                AppLog.i("PhoneHotspot: joined \"$ssid\"; looking for the head unit server on the phone")
                return true
            }
            try { Thread.sleep(POLL_MS) } catch (e: InterruptedException) { return false }
        }
        AppLog.w("PhoneHotspot: not on \"$ssid\" after ${JOIN_WAIT_MS / 1000}s. Check the phone's " +
            "hotspot is on, set to 2.4 GHz, and that the name and password match.")
        return false
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun isOn(wifi: WifiManager, ssid: String): Boolean = try {
        wifi.connectionInfo?.ssid?.trim('"') == ssid && (wifi.connectionInfo?.ipAddress ?: 0) != 0
    } catch (e: Exception) { false }

    /** Android 7–9: apps may add and select a network directly. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun joinLegacy(wifi: WifiManager, ssid: String, password: String): Boolean = try {
        val quoted = "\"$ssid\""
        val existing = wifi.configuredNetworks?.firstOrNull { it.SSID == quoted }
        val id = existing?.networkId ?: wifi.addNetwork(WifiConfiguration().apply {
            SSID = quoted
            if (password.isEmpty()) {
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            } else {
                preSharedKey = "\"$password\""
            }
        })
        if (id < 0) {
            AppLog.w("PhoneHotspot: Android refused to add \"$ssid\"")
            false
        } else {
            wifi.disconnect()
            val ok = wifi.enableNetwork(id, true) && wifi.reconnect()
            AppLog.i("PhoneHotspot: asked to join \"$ssid\" (network $id): $ok")
            ok
        }
    } catch (e: Exception) {
        AppLog.w("PhoneHotspot: join failed", e)
        false
    }

    /**
     * Android 10+: apps cannot pick the network themselves. A suggestion is remembered by the
     * system (it asks the user to allow it once) and joined whenever the phone's hotspot is in range.
     */
    private fun suggest(wifi: WifiManager, ssid: String, password: String): Boolean = try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val builder = WifiNetworkSuggestion.Builder().setSsid(ssid)
        if (password.isNotEmpty()) builder.setWpa2Passphrase(password)
        val status = wifi.addNetworkSuggestions(listOf(builder.build()))
        val ok = status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS ||
            status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE
        AppLog.i("PhoneHotspot: suggested \"$ssid\" to Android (status $status). If Android asks, allow it.")
        ok
    } catch (e: Exception) {
        AppLog.w("PhoneHotspot: suggestion failed", e)
        false
    }
}
