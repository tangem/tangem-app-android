package com.tangem.features.polymarket.impl.main.addfunds

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel

internal class PolymarketAddFundsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext {

    private val model: PolymarketAddFundsModel = getOrCreateModel(params = params)

    fun dismiss() {
        model.onDismiss()
    }

    data class Params(
        val listener: Listener,
    )

    interface Listener {
        fun onSwapClick()
        fun onReceiveClick()
        fun onDismissAddFunds()
    }
}