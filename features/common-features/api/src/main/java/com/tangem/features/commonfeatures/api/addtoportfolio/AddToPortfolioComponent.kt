package com.tangem.features.commonfeatures.api.addtoportfolio

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
        // todo swap add new onSuccess with full data of added token
        fun onSuccess(addedToken: CryptoCurrency)
    }

    interface Factory : ComponentFactory<Params, AddToPortfolioComponent>
}