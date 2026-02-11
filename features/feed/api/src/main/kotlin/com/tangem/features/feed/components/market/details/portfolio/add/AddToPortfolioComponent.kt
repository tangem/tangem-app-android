package com.tangem.features.feed.components.market.details.portfolio.add

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency

interface AddToPortfolioComponent : ComposableBottomSheetComponent {

    data class Params(
        val addToPortfolioManager: AddToPortfolioManager,
        val callback: Callback,
        val shouldSkipTokenActionsScreen: Boolean = false,
    )

    interface Callback {
        fun onDismiss()
        fun onSuccess(addedToken: CryptoCurrency)
    }

    interface Factory : ComponentFactory<Params, AddToPortfolioComponent>
}