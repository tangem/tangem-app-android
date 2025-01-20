package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.intentHandler.IntentHandler
import com.tangem.tap.features.intentHandler.AffectsNavigation
import com.tangem.tap.features.welcome.redux.WelcomeAction
import com.tangem.tap.store
import kotlinx.coroutines.CoroutineScope

/**
[REDACTED_AUTHOR]
 */
class BackgroundScanIntentHandler(
    private val hasSavedUserWalletsProvider: () -> Boolean,
    private val scope: CoroutineScope,
) : IntentHandler, AffectsNavigation {

    private val nfcActions = arrayOf(
        NfcAdapter.ACTION_NDEF_DISCOVERED,
        NfcAdapter.ACTION_TECH_DISCOVERED,
        NfcAdapter.ACTION_TAG_DISCOVERED,
    )

    override fun handleIntent(intent: Intent?, isFromForeground: Boolean): Boolean {
        if (isFromForeground) return true
        if (intent == null || intent.action !in nfcActions) return false

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        if (tag == null) return false

        intent.action = null
        if (hasSavedUserWalletsProvider.invoke()) {
            store.dispatchOnMain(WelcomeAction.ProceedWithCard)
        } else {
            store.dispatchOnMain(HomeAction.ReadCard(scope = scope))
        }

        return true
    }
}