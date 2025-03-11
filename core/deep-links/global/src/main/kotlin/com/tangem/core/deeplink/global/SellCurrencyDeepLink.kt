package com.tangem.core.deeplink.global

import com.tangem.core.deeplink.DeepLink

class SellCurrencyDeepLink(
    val onReceive: (data: Data) -> Unit,
    shouldHandleDelayed: Boolean,
) : DeepLink(shouldHandleDelayed) {

    override val uri: String = "tangem://redirect_sell"

    override fun onReceive(params: Map<String, String>) {
        val data = Data(
            transactionId = params["transactionId"] ?: return,
            baseCurrencyAmount = params["baseCurrencyAmount"] ?: return,
            depositWalletAddress = params["depositWalletAddress"] ?: return,
            currencyId = params["currency_id"] ?: return,
            depositWalletAddressTag = params["depositWalletAddressTag"],
        )

        onReceive(data)
    }

    data class Data(
        val transactionId: String,
        val baseCurrencyAmount: String,
        val depositWalletAddress: String,
        val currencyId: String,
        val depositWalletAddressTag: String?,
    )
}