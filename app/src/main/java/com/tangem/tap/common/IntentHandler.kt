package com.tangem.tap.common

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.tangem.tap.common.extensions.removePrefixOrNull
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import timber.log.Timber

class IntentHandler {

    fun handleWalletConnectLink(intent: Intent?) {
        val wcUri = when (intent?.scheme) {
            WalletConnectManager.WC_SCHEME -> {
                intent.data?.toString()
            }
            TANGEM_SCHEME -> {
                intent.data?.toString()?.removePrefixOrNull(TANGEM_WC_PREFIX)
            }
            else -> {
                null
            }
        }
        if (wcUri != null) {
            store.dispatch(WalletConnectAction.HandleDeepLink(wcUri))
        }
    }

    fun handleBackgroundScan(intent: Intent?) {
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

    fun handleSellCurrencyCallback(intent: Intent?) {
        try {
            val transactionID =
                intent?.data?.getQueryParameter(TRANSACTION_ID_PARAM) ?: return
            val currency =
                intent.data?.getQueryParameter(CURRENCY_CODE_PARAM) ?: return
            val amount =
                intent.data?.getQueryParameter(CURRENCY_AMOUNT_PARAM) ?: return
            val destinationAddress =
                intent.data?.getQueryParameter(DEPOSIT_WALLET_ADDRESS_PARAM)
                    ?: return

            Timber.d("MoonPay Sell: $amount $currency to $destinationAddress")

            store.dispatch(
                WalletAction.TradeCryptoAction.SendCrypto(
                    currencyId = currency,
                    amount = amount,
                    destinationAddress = destinationAddress,
                    transactionId = transactionID,
                ),
            )
        } catch (exception: Exception) {
            Timber.d("Not MoonPay URL")
        }
    }

    companion object {
        private const val TRANSACTION_ID_PARAM = "transactionId"
        private const val CURRENCY_CODE_PARAM = "baseCurrencyCode"
        private const val CURRENCY_AMOUNT_PARAM = "baseCurrencyAmount"
        private const val DEPOSIT_WALLET_ADDRESS_PARAM = "depositWalletAddress"
        private const val TANGEM_SCHEME = "tangem"
        private const val TANGEM_WC_PREFIX = "tangem://wc?uri="
    }
}
