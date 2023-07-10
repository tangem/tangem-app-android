package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import com.tangem.tap.features.intentHandler.IntentHandler
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class SellCurrencyIntentHandler : IntentHandler {

    override suspend fun handleIntent(intent: Intent?): Boolean {
        return try {
            val intentData = intent?.data ?: return false
            val transactionID = intentData.getQueryParameter(TRANSACTION_ID_PARAM) ?: return false
            val currency = intentData.getQueryParameter(CURRENCY_CODE_PARAM) ?: return false
            val amount = intentData.getQueryParameter(CURRENCY_AMOUNT_PARAM) ?: return false
            val destinationAddress = intentData.getQueryParameter(DEPOSIT_WALLET_ADDRESS_PARAM) ?: return false

            Timber.d("MoonPay Sell: $amount $currency to $destinationAddress")
            store.dispatch(
                WalletAction.TradeCryptoAction.SendCrypto(
                    currencyId = currency,
                    amount = amount,
                    destinationAddress = destinationAddress,
                    transactionId = transactionID,
                ),
            )
            true
        } catch (exception: Exception) {
            Timber.d("Not MoonPay URL")
            false
        }
    }

    private companion object {
        private const val TRANSACTION_ID_PARAM = "transactionId"
        private const val CURRENCY_CODE_PARAM = "baseCurrencyCode"
        private const val CURRENCY_AMOUNT_PARAM = "baseCurrencyAmount"
        private const val DEPOSIT_WALLET_ADDRESS_PARAM = "depositWalletAddress"
    }
}