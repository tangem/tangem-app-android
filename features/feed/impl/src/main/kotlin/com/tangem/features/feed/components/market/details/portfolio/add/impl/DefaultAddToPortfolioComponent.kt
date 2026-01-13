package com.tangem.features.feed.components.market.details.portfolio.add.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.add.impl.model.AddToPortfolioModel
import com.tangem.features.feed.components.market.details.portfolio.add.impl.model.AddToPortfolioRoutes
import com.tangem.features.feed.impl.R
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
        val stack by childStack.subscribeAsState()
        val contentStack = remember { mutableStateOf(stack) }
        val currentRoute = stack.active.configuration
        val isNotEmpty = currentRoute != AddToPortfolioRoutes.Empty
        if (isNotEmpty) {
            contentStack.value = stack
        }

        TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
            scrollableContent = false,
            onBack = ::onBack,
            config = TangemBottomSheetConfig(
                isShown = isNotEmpty,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = { state ->
                AnimatedContent(targetState = contentStack.value) { stack ->
                    BottomSheetTitle(
                        stack = stack,
                        onBackClick = ::onBack,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            content = { state ->
                AnimatedContent(targetState = contentStack.value) { stack ->
                    val paddingModifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    )
                    val isScrollableContent = when (stack.active.configuration) {
                        AddToPortfolioRoutes.PortfolioSelector -> false
                        AddToPortfolioRoutes.AddToken,
                        AddToPortfolioRoutes.Empty,
                        is AddToPortfolioRoutes.NetworkSelector,
                        AddToPortfolioRoutes.TokenActions,
                        -> true
                    }
                    if (isScrollableContent) {
                        Column(
                            modifier = paddingModifier.verticalScroll(rememberScrollState()),
                        ) {
                            stack.active.instance.Content(modifier = Modifier)
                        }
                    } else {
                        stack.active.instance.Content(modifier = paddingModifier)
                    }
                }
            },
        )
    }

    @Composable
    private fun BottomSheetTitle(
        stack: ChildStack<AddToPortfolioRoutes, ComposableContentComponent>,
        onBackClick: (() -> Unit),
        modifier: Modifier = Modifier,
    ) {
        val title: TextReference = when (stack.active.configuration) {
            AddToPortfolioRoutes.AddToken -> resourceReference(R.string.common_add_token)
            AddToPortfolioRoutes.Empty -> TextReference.EMPTY
            is AddToPortfolioRoutes.NetworkSelector -> resourceReference(R.string.common_choose_network)
            AddToPortfolioRoutes.TokenActions -> resourceReference(R.string.common_get_token)
            AddToPortfolioRoutes.PortfolioSelector -> (stack.active.instance as PortfolioSelectorComponent)
                .title.collectAsStateWithLifecycle().value
        }
        val startIconRes: Int?
        val endIconRes: Int?
        if (stack.backStack.isNotEmpty()) {
            startIconRes = R.drawable.ic_back_24
            endIconRes = null
        } else {
            startIconRes = null
            endIconRes = R.drawable.ic_close_24
        }
        TangemModalBottomSheetTitle(
            modifier = modifier,
            title = title,
            startIconRes = startIconRes,
            endIconRes = endIconRes,
            onStartClick = onBackClick,
            onEndClick = onBackClick,
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