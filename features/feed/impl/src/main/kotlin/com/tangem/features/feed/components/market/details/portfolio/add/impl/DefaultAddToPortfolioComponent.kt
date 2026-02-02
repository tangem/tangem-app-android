package com.tangem.features.feed.components.market.details.portfolio.add.impl

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.add.impl.model.AddToPortfolioModel
import com.tangem.features.feed.components.market.details.portfolio.add.impl.model.AddToPortfolioRoutes
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddToPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddToPortfolioComponent.Params,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    addTokenComponentFactory: AddTokenComponent.Factory,
    tokenActionsComponentFactory: TokenActionsComponent.Factory,
    private val chooseNetworkComponentFactory: ChooseNetworkComponent.Factory,
) : AppComponentContext by context, AddToPortfolioComponent {

    private val model: AddToPortfolioModel = getOrCreateModel(params)

    private val portfolioSelectorComponent: PortfolioSelectorComponent = portfolioSelectorComponentFactory.create(
        context = child("portfolioSelectorComponent"),
        params = PortfolioSelectorComponent.Params(
            portfolioFetcher = model.portfolioFetcher,
            controller = model.portfolioSelectorController,
        ),
    )

    private val addTokenComponent: AddTokenComponent = addTokenComponentFactory.create(
        context = child("addTokenComponent"),
        params = AddTokenComponent.Params(
            eventBuilder = model.eventBuilder,
            callbacks = model,
            selectedPortfolio = model.selectedPortfolio,
            selectedNetwork = model.selectedNetwork,
        ),
    )

    private val tokenActionsComponent: TokenActionsComponent = tokenActionsComponentFactory.create(
        context = child("tokenActionsComponent"),
        params = TokenActionsComponent.Params(
            eventBuilder = model.eventBuilder,
            callbacks = model,
            data = model.tokenActionsData,
        ),
    )

    private val childStack = childStack(
        key = "addToPortfolioStack",
        handleBackButton = true,
        source = model.navigation,
        serializer = AddToPortfolioRoutes.serializer(),
        initialStack = { model.currentStack },
        childFactory = ::contentChild,
    )

    private fun onBack() {
        if (childStack.backStack.isNotEmpty()) model.navigation.pop() else dismiss()
    }

    override fun dismiss() {
        params.callback.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        AddToPortfolioBottomSheet(
            childStack = childStack.subscribeAsState(),
            onBack = ::onBack,
            onDismiss = ::dismiss,
        )
    }

    private fun contentChild(
        config: AddToPortfolioRoutes,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        AddToPortfolioRoutes.AddToken -> addTokenComponent
        AddToPortfolioRoutes.PortfolioSelector -> portfolioSelectorComponent
        AddToPortfolioRoutes.TokenActions -> tokenActionsComponent
        AddToPortfolioRoutes.Empty -> ComposableContentComponent.EMPTY
        is AddToPortfolioRoutes.NetworkSelector -> chooseNetworkComponentFactory.create(
            context = childByContext(componentContext),
            params = ChooseNetworkComponent.Params(
                selectedPortfolio = config.selectedPortfolio,
                callbacks = model,
            ),
        )
    }

    @AssistedFactory
    interface Factory : AddToPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddToPortfolioComponent.Params,
        ): DefaultAddToPortfolioComponent
    }
}