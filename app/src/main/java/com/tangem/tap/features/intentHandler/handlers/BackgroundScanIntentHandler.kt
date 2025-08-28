package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import com.tangem.common.routing.entity.InitScreenLaunchMode

/**
[REDACTED_AUTHOR]
 */
class BackgroundScanIntentHandler {

    private val nfcActions = arrayOf(
        NfcAdapter.ACTION_NDEF_DISCOVERED,
        NfcAdapter.ACTION_TECH_DISCOVERED,
        NfcAdapter.ACTION_TAG_DISCOVERED,
    )

    fun getInitScreenLaunchMode(intent: Intent?): InitScreenLaunchMode {
        return if (shouldOpenScanCard(intent)) {
            InitScreenLaunchMode.WithCardScan
        } else {
            InitScreenLaunchMode.Standard
        }
    }

    private fun shouldOpenScanCard(intent: Intent?): Boolean {
        if (intent == null || intent.action !in nfcActions) return false

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        intent.action = null

        return tag != null
    }
}