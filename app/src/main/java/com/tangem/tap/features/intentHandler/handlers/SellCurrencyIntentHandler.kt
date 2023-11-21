package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import com.tangem.tap.features.intentHandler.IntentHandler

/**
 * @author Anton Zhilenkov on 04.06.2023.
 */
class SellCurrencyIntentHandler : IntentHandler {

    override fun handleIntent(intent: Intent?): Boolean {
        // FIXME: https://tangem.atlassian.net/browse/AND-5311
        // return try {
        //     val intentData = intent?.data ?: return false
        //     val transactionID = intentData.getQueryParameter(TRANSACTION_ID_PARAM) ?: return false
        //     val currency = intentData.getQueryParameter(CURRENCY_CODE_PARAM) ?: return false
        //     val amount = intentData.getQueryParameter(CURRENCY_AMOUNT_PARAM) ?: return false
        //     val destinationAddress = intentData.getQueryParameter(DEPOSIT_WALLET_ADDRESS_PARAM) ?: return false
        //
        //     Timber.d("MoonPay Sell: $amount $currency to $destinationAddress")
        //     store.dispatchOnMain(
        //         TradeCryptoAction.SendCrypto(
        //             currencyId = currency,
        //             amount = amount,
        //             destinationAddress = destinationAddress,
        //             transactionId = transactionID,
        //         ),
        //     )
        //     true
        // } catch (exception: Exception) {
        //     Timber.d("Not MoonPay URL")
        //     false
        // }

        return false
    }

    // private companion object {
    //     private const val TRANSACTION_ID_PARAM = "transactionId"
    //     private const val CURRENCY_CODE_PARAM = "baseCurrencyCode"
    //     private const val CURRENCY_AMOUNT_PARAM = "baseCurrencyAmount"
    //     private const val DEPOSIT_WALLET_ADDRESS_PARAM = "depositWalletAddress"
    // }
}
