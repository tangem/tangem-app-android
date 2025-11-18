package com.tangem.features.yield.supply.api

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.details.TokenAction

interface YieldSupplyDepositedWarningComponent : ComposableBottomSheetComponent {

    data class Params(
        val cryptoCurrency: CryptoCurrency,
        val tokenAction: TokenAction,
        val modelCallback: ModelCallback,
        val onDismiss: () -> Unit,
    )

    interface ModelCallback {
        fun onYieldSupplyWarningAcknowledged(tokenAction: TokenAction)
    }

    interface Factory : ComponentFactory<Params, YieldSupplyDepositedWarningComponent> {
        override fun create(context: AppComponentContext, params: Params): YieldSupplyDepositedWarningComponent
    }
}