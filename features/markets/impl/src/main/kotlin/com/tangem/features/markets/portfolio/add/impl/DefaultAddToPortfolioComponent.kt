package com.tangem.features.markets.portfolio.add.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.markets.portfolio.add.api.AddToPortfolioComponent
import com.tangem.features.markets.portfolio.add.impl.model.AddToPortfolioModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddToPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddToPortfolioComponent.Params,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : AppComponentContext by context, AddToPortfolioComponent {

    private val model: AddToPortfolioModel = getOrCreateModel(params)

    val portfolioSelectorComponent = portfolioSelectorComponentFactory.create(
        context = child("portfolioSelectorComponent"),
        params = PortfolioSelectorComponent.Params(
            portfolioFetcher = model.portfolioFetcher,
            controller = model.portfolioSelectorController,
            onDismiss = {},
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        TODO("Not yet implemented")
    }

    @AssistedFactory
    interface Factory : AddToPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddToPortfolioComponent.Params,
        ): DefaultAddToPortfolioComponent
    }
}