package com.tangem.tap.common

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import com.tangem.tap.common.extensions.removePrefixOrNull
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.welcome.redux.WelcomeAction
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class IntentHandler {

    private val nfcActions = arrayOf(
        NfcAdapter.ACTION_NDEF_DISCOVERED,
        NfcAdapter.ACTION_TECH_DISCOVERED,
        NfcAdapter.ACTION_TAG_DISCOVERED,
    )

    fun handleIntent(intent: Intent?, hasSavedUserWallets: Boolean) {
        handleBackgroundScan(intent, hasSavedUserWallets)
        handleWalletConnectLink(intent)
        handleSellCurrencyCallback(intent)
    }

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

    fun handleBackgroundScan(intent: Intent?, hasSavedUserWallets: Boolean): Boolean {
        if (intent == null || intent.action !in nfcActions) return false

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        if (tag == null) return false

        intent.action = null
        if (hasSavedUserWallets) {
// [REDACTED_TODO_COMMENT]
            scope.launch {
                delay(timeMillis = 200)
                store.dispatch(WelcomeAction.ProceedWithCard)
            }
        } else {
            store.dispatch(HomeAction.ReadCard())
        }

        return true
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
