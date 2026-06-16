package com.tangem.features.commonfeatures.impl.userportfolio

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.commonfeatures.impl.userportfolio.model.UserPortfolioUM
import kotlinx.coroutines.flow.StateFlow

internal interface UserPortfolioComponent : ComposableContentComponent {

    data class Params(
        val uiState: StateFlow<UserPortfolioUM?>,
        val callbacks: Callbacks,
    )

    interface Callbacks {
        fun onContinueFromUserPortfolio()
    }

    interface Factory : ComponentFactory<Params, UserPortfolioComponent>
}