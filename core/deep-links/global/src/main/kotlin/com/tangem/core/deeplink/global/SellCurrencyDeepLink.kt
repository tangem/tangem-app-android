package com.tangem.core.deeplink.global

import com.tangem.core.deeplink.DeepLink

class SellCurrencyDeepLink(val onReceive: (data: Data) -> Unit) : DeepLink {

    override val uri: String = "tangem://sell-request.tangem.com"

    override fun onReceive(params: Map<String, String>) {
        val data = Data(
            transactionId = params["transactionId"] ?: return,
            baseCurrencyAmount = params["baseCurrencyAmount"] ?: return,
            depositWalletAddress = params["depositWalletAddress"] ?: return,
        )

        onReceive(data)
    }

    data class Data(
        val transactionId: String,
        val baseCurrencyAmount: String,
        val depositWalletAddress: String,
    )
}