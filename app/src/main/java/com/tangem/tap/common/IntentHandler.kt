package com.tangem.tap.common

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.store

class IntentHandler {

    fun handleIntent(intent: Intent?) {
        handleBackgroundScan(intent)
        handleWalletConnectLink(intent)
    }

    private fun handleBackgroundScan(intent: Intent?) {
        if (intent != null && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
                    NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
                    )
        ) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                intent.action = null
                store.dispatch(NavigationAction.NavigateTo(AppScreen.Home))
                store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                store.dispatch(HomeAction.ReadCard)
            }
        }
    }

    private fun handleWalletConnectLink(intent: Intent?) {
        if (intent?.scheme == WalletConnectManager.WC_SCHEME) {
            store.dispatch(WalletConnectAction.HandleDeepLink(intent.data?.toString()))
        }
    }

}