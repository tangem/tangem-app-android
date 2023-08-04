package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.intentHandler.IntentHandler
import com.tangem.tap.features.welcome.redux.WelcomeAction
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
[REDACTED_AUTHOR]
 */
class BackgroundScanIntentHandler(
    private val hasSavedUserWalletsProvider: () -> Boolean,
) : IntentHandler {

    private val nfcActions = arrayOf(
        NfcAdapter.ACTION_NDEF_DISCOVERED,
        NfcAdapter.ACTION_TECH_DISCOVERED,
        NfcAdapter.ACTION_TAG_DISCOVERED,
    )

    override suspend fun handleIntent(intent: Intent?): Boolean {
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
            // TODO: Remove delay after [REDACTED_JIRA]
            scope.launch {
                delay(timeMillis = 200)
                store.dispatchWithMain(WelcomeAction.ProceedWithCard)
            }
        } else {
            store.dispatchWithMain(HomeAction.ReadCard())
        }

        return true
    }
}