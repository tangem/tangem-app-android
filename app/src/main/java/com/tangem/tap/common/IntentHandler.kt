package com.tangem.tap.common

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.topup.TradeCryptoHelper
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import timber.log.Timber

class IntentHandler {

    fun handleIntent(intent: Intent?) {
        handleBackgroundScan(intent)
        handleWalletConnectLink(intent)
        handleSellCurrencyCallback(intent)
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

    private fun handleSellCurrencyCallback(intent: Intent?) {
        val transactionID =
            intent?.data?.getQueryParameter(TradeCryptoHelper.TRANSACTION_ID_PARAM) ?: return
        val currency =
            intent.data?.getQueryParameter(TradeCryptoHelper.CURRENCY_CODE_PARAM) ?: return
        val amount =
            intent.data?.getQueryParameter(TradeCryptoHelper.CURRENCY_AMOUNT_PARAM) ?: return
        val destinationAddress =
            intent.data?.getQueryParameter(TradeCryptoHelper.DEPOSIT_WALLET_ADDRESS_PARAM)
                ?: return

        Timber.d("MoonPay Sell: $amount $currency to $destinationAddress")

        store.dispatch(WalletAction.TradeCryptoAction.SendCrypto(
            currencyId = currency,
            amount = amount,
            destinationAddress = destinationAddress,
            transactionId = transactionID
        ))
    }


}