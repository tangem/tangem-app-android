package com.tangem.features.commonfeatures.impl.addtoportfolio

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioPreselectedDataComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioPreselectedDataModel
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioRoutes
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.flowOf

internal class DefaultAddToPortfolioPreselectedDataComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddToPortfolioPreselectedDataComponent.Params,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    addTokenComponentFactory: AddTokenComponent.Factory,
) : AppComponentContext by context, AddToPortfolioPreselectedDataComponent {

    private val model: AddToPortfolioPreselectedDataModel = getOrCreateModel(params)

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
            eventBuilder = flowOf(model.eventBuilder),
            callbacks = model,
            selectedPortfolio = model.selectedPortfolio,
            selectedNetwork = model.selectedNetwork,
        ),
    )

    private val childStack = childStack(
        key = "addToPortfolioFromEarnStack",
        handleBackButton = true,
        source = model.navigation,
        serializer = AddToPortfolioRoutes.serializer(),
        initialStack = { model.currentStack },
        childFactory = { configuration, _ ->
            contentChild(configuration)
        },
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

    private fun contentChild(config: AddToPortfolioRoutes): ComposableContentComponent = when (config) {
        AddToPortfolioRoutes.AddToken -> addTokenComponent
        AddToPortfolioRoutes.PortfolioSelector -> portfolioSelectorComponent
        AddToPortfolioRoutes.TokenActions -> ComposableContentComponent.EMPTY
        AddToPortfolioRoutes.Empty -> ComposableContentComponent.EMPTY
        is AddToPortfolioRoutes.NetworkSelector -> ComposableContentComponent.EMPTY
        AddToPortfolioRoutes.UserPortfolio -> ComposableContentComponent.EMPTY
    }

    @AssistedFactory
    interface Factory : AddToPortfolioPreselectedDataComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddToPortfolioPreselectedDataComponent.Params,
        ): DefaultAddToPortfolioPreselectedDataComponent
    }
}