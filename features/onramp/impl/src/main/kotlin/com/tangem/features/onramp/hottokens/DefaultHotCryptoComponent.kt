package com.tangem.features.onramp.hottokens

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
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.*
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.onramp.hottokens.model.HotCryptoModel
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddToPortfolioComponent
import com.tangem.features.onramp.hottokens.portfolio.OnrampAddTokenComponent
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddToPortfolioBSConfig
import com.tangem.features.onramp.hottokens.portfolio.entity.OnrampAddTokenRoute
import com.tangem.features.onramp.hottokens.ui.HotCrypto
import com.tangem.features.onramp.impl.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultHotCryptoComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: HotCryptoComponent.Params,
    private val onrampAddToPortfolioComponentFactory: OnrampAddToPortfolioComponent.Factory,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    addTokenComponentFactory: OnrampAddTokenComponent.Factory,
) : HotCryptoComponent, AppComponentContext by context {

    private val model: HotCryptoModel = getOrCreateModel(params)

    private val portfolioSelectorComponent: PortfolioSelectorComponent? by lazy {
        if (accountsFeatureToggles.isFeatureEnabled) {
            portfolioSelectorComponentFactory.create(
                context = child("portfolioSelectorComponent"),
                params = PortfolioSelectorComponent.Params(
                    portfolioFetcher = requireNotNull(model.portfolioFetcher),
                    controller = model.portfolioSelectorController,
                ),
            )
        } else {
            null
        }
    }
    private val addTokenComponent: OnrampAddTokenComponent? by lazy {
        if (accountsFeatureToggles.isFeatureEnabled) {
            addTokenComponentFactory.create(
                context = child("addTokenComponent"),
                params = OnrampAddTokenComponent.Params(
                    callbacks = model,
                    tokenToAdd = model.hotCryptoToAddDataFlow,
                ),
            )
        } else {
            null
        }
    }

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    private val childStack = childStack(
        key = "addHotCryptoToPortfolioStack",
        handleBackButton = true,
        source = model.bottomSheetNavigationV2,
        serializer = OnrampAddTokenRoute.serializer(),
        initialConfiguration = OnrampAddTokenRoute.Empty,
        childFactory = { config, _ -> contentChild(config) },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        HotCrypto(state, modifier)

        bottomSheet.child?.instance?.BottomSheet()
        if (accountsFeatureToggles.isFeatureEnabled) {
            AddHotCryptoBottomSheet()
        }
    }

    @Composable
    private fun AddHotCryptoBottomSheet() {
        val stack by childStack.subscribeAsState()
        val contentStack = remember { mutableStateOf(stack) }
        val currentRoute = stack.active.configuration
        val isNotEmpty = currentRoute != OnrampAddTokenRoute.Empty
        if (isNotEmpty) {
            contentStack.value = stack
        }

        fun dismiss() = model.bottomSheetNavigationV2.replaceAll(OnrampAddTokenRoute.Empty)
        fun onBack() = if (childStack.backStack.isNotEmpty()) model.bottomSheetNavigationV2.pop() else dismiss()

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
                        OnrampAddTokenRoute.PortfolioSelector -> false
                        OnrampAddTokenRoute.AddToken,
                        OnrampAddTokenRoute.Empty,
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
        stack: ChildStack<OnrampAddTokenRoute, ComposableContentComponent>,
        onBackClick: (() -> Unit),
        modifier: Modifier = Modifier,
    ) {
        val title: TextReference = when (stack.active.configuration) {
            OnrampAddTokenRoute.AddToken -> resourceReference(R.string.common_add_token)
            OnrampAddTokenRoute.Empty -> TextReference.EMPTY
            OnrampAddTokenRoute.PortfolioSelector -> (stack.active.instance as PortfolioSelectorComponent)
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

    private fun bottomSheetChild(
        config: OnrampAddToPortfolioBSConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        return when (config) {
            is OnrampAddToPortfolioBSConfig.AddToPortfolio -> {
                onrampAddToPortfolioComponentFactory.create(
                    context = childByContext(componentContext),
                    params = OnrampAddToPortfolioComponent.Params(
                        userWalletId = params.userWalletId,
                        cryptoCurrency = config.cryptoCurrency,
                        currencyIconState = config.currencyIconState,
                        onSuccessAdding = config.onSuccessAdding,
                        onDismiss = model.bottomSheetNavigation::dismiss,
                    ),
                )
            }
        }
    }

    private fun contentChild(config: OnrampAddTokenRoute): ComposableContentComponent = when (config) {
        OnrampAddTokenRoute.AddToken -> requireNotNull(addTokenComponent)
        OnrampAddTokenRoute.PortfolioSelector -> requireNotNull(portfolioSelectorComponent)
        OnrampAddTokenRoute.Empty -> ComposableContentComponent.EMPTY
    }

    @AssistedFactory
    interface Factory : HotCryptoComponent.Factory {
        override fun create(context: AppComponentContext, params: HotCryptoComponent.Params): DefaultHotCryptoComponent
    }
}