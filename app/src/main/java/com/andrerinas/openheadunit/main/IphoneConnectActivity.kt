package com.andrerinas.openheadunit.main

import android.app.Activity
import android.os.Bundle
import com.andrerinas.openheadunit.utils.PhoneHub

/**
 * A manual "connect iPhone" launcher. Tapping its icon opens the iPhone connection at once
 * (AirPlay, or the CarPlay app chosen in Settings) without waiting for a USB plug or Bluetooth
 * connect. Handy to start it by hand while driving. It does its work and closes immediately.
 */
class IphoneConnectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            PhoneHub.handOffManual(this)
        } finally {
            finish()
        }
    }
}
