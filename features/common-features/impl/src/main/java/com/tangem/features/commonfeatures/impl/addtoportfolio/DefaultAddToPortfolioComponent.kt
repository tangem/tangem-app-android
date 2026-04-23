package com.tangem.features.commonfeatures.impl.addtoportfolio

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
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioModel
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioRoutes
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.UserPortfolioComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultAddToPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddToPortfolioComponent.Params,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    addTokenComponentFactory: AddTokenComponent.Factory,
    tokenActionsComponentFactory: TokenActionsComponent.Factory,
    private val chooseNetworkComponentFactory: ChooseNetworkComponent.Factory,
    private val userPortfolioComponentFactory: UserPortfolioComponent.Factory,
) : AppComponentContext by context, AddToPortfolioComponent {

    private val model: AddToPortfolioModel = getOrCreateModel(params)

    private val portfolioSelectorComponent: PortfolioSelectorComponent = portfolioSelectorComponentFactory.create(
        context = child("portfolioSelectorComponent"),
        params = PortfolioSelectorComponent.Params(
            portfolioFetcher = model.portfolioFetcher,
            controller = model.portfolioSelectorController,
        ),
    )

    private val addTokenComponent: AddTokenComponent by lazy {
        addTokenComponentFactory.create(
            context = child("addTokenComponent"),
            params = AddTokenComponent.Params(
                eventBuilder = model.eventBuilder,
                callbacks = model,
                selectedPortfolio = model.selectedPortfolio,
                selectedNetwork = model.selectedNetwork,
            ),
        )
    }

    private val tokenActionsComponent: TokenActionsComponent by lazy {
        tokenActionsComponentFactory.create(
            context = child("tokenActionsComponent"),
            params = TokenActionsComponent.Params(
                eventBuilder = model.eventBuilder,
                callbacks = model,
                data = model.tokenActionsData,
            ),
        )
    }

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
        model.addToPortfolioManager.onDismiss()
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
        AddToPortfolioRoutes.UserPortfolio -> createUserPortfolioComponent(componentContext)
        is AddToPortfolioRoutes.NetworkSelector -> chooseNetworkComponentFactory.create(
            context = childByContext(componentContext),
            params = ChooseNetworkComponent.Params(
                selectedPortfolio = config.selectedPortfolio,
                callbacks = model,
            ),
        )
    }

    private fun createUserPortfolioComponent(componentContext: ComponentContext): ComposableContentComponent {
        return when (model.addToPortfolioManager.settings.launchMode) {
            AddToPortfolioManager.LaunchMode.DirectAdd -> ComposableContentComponent.EMPTY
            is AddToPortfolioManager.LaunchMode.ViaUserPortfolio -> userPortfolioComponentFactory.create(
                context = childByContext(componentContext),
                params = UserPortfolioComponent.Params(
                    uiState = model.userPortfolioStateController.uiState,
                    callbacks = model,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : AddToPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddToPortfolioComponent.Params,
        ): DefaultAddToPortfolioComponent
    }
}