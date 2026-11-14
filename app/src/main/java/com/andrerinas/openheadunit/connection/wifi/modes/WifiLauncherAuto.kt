package com.andrerinas.openheadunit.connection.wifi.modes

import com.andrerinas.openheadunit.connection.ConnectionStage
import com.andrerinas.openheadunit.connection.ConnectionStageTracker
import com.andrerinas.openheadunit.connection.wifi.WifiLauncher
import com.andrerinas.openheadunit.connection.wifi.WifiLauncherManager
import com.andrerinas.openheadunit.connection.wifi.WifiLauncherMode
import com.andrerinas.openheadunit.connection.wifi.WifiLauncherStopSequence
import com.andrerinas.openheadunit.connection.wifi.PhoneHotspotJoiner

class WifiLauncherAuto(
    manager: WifiLauncherManager
) : WifiLauncher(manager) {

    override val mode = WifiLauncherMode.AUTO

    override fun hasSameStartConfiguration(launcher: WifiLauncher): Boolean {
        return launcher is WifiLauncherAuto
    }

    override fun hasWifiDirect() = false

    override fun hostsOwnAccessPoint() = false

    override fun hasWirelessServer() = true

    override fun hasLocalDiscovery() = true

    override fun start(noInfoToasts: Boolean) {
        // Auto discovery for standard server mode via NSD/mDNS
        // #startDiscovery(oneShot = false) handled by SharedServices
        ConnectionStageTracker.report(ConnectionStage.SEARCHING)
        // Phone hotspot client mode: join the phone's hotspot first. The Wi-Fi join kicks a
        // discovery sweep, which dials the gateway (the phone) on port 5277.
        val context = manager.service
        val settings = com.andrerinas.openheadunit.utils.Settings(context)
        if (settings.joinsPhoneHotspot) {
            Thread({ PhoneHotspotJoiner.ensureJoined(context, settings) }, "phone-hotspot-join").start()
        }
    }

    override fun stop(seq: WifiLauncherStopSequence) {
    }
}
