package com.tangem.features.feed.components.market.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PreselectedTokenDetailsSection
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.commonfeatures.api.managefunds.ManageFundsComponent
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolioblock.PortfolioBlockComponent
import com.tangem.features.feed.components.market.details.portfolioblock.PortfolioBlockParentClickIntents
import com.tangem.features.feed.model.market.details.MarketsTokenDetailsModel
import com.tangem.features.feed.model.market.details.analytics.MarketDetailsAnalyticsEvent
import com.tangem.features.feed.model.market.details.state.TokenNetworksState
import com.tangem.features.feed.ui.LocalIsOpenedInBottomSheet
import com.tangem.features.feed.ui.market.detailed.MarketsTokenDetailsContent
import com.tangem.features.feed.ui.market.detailed.MarketsTokenDetailsTopBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Suppress("LongParameterList")
internal class DefaultMarketsTokenDetailsComponent(
    appComponentContext: AppComponentContext,
    analyticsEventHandler: AnalyticsEventHandler,
    designFeatureToggles: DesignFeatureToggles,
    portfolioComponentFactory: MarketsPortfolioComponent.Factory,
    portfolioBlockComponentFactory: PortfolioBlockComponent.Factory,
    val params: Params,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
    private val manageFundsComponentFactory: ManageFundsComponent.Factory,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    // applying l2 compatibility
    private val updatedParams = params.copy(
        token = params.token.copy(
            id = CryptoCurrency.RawID(getTokenIdIfL2Network(params.token.id.value)),
        ),
    )
    private val analyticsParams = params.analyticsParams
    private val model: MarketsTokenDetailsModel = getOrCreateModel(updatedParams)

    private val portfolioComponent: MarketsPortfolioComponent? =
        if (updatedParams.shouldShowPortfolio && !designFeatureToggles.isRedesignEnabled) {
            portfolioComponentFactory.create(
                context = child("my_portfolio"),
                params = MarketsPortfolioComponent.Params(
                    updatedParams.token,
                    analyticsParams = analyticsParams?.source?.let { MarketsPortfolioComponent.AnalyticsParams(it) },
                ),
            )
        } else {
            null
        }

    private val portfolioBlockComponent: PortfolioBlockComponent? =
        if (updatedParams.shouldShowPortfolio && designFeatureToggles.isRedesignEnabled) {
            portfolioBlockComponentFactory.create(
                context = child("portfolio_block"),
                params = PortfolioBlockComponent.Params(token = updatedParams.token),
                parentRouter = object : PortfolioBlockParentClickIntents {
                    override fun openAddToPortfolioDirect() {
                        model.openAddToPortfolio()
                    }

                    override fun openAddToPortfolioViaUserPortfolio(rawCurrencyId: CryptoCurrency.RawID) {
                        model.openAddToPortfolioViaUserPortfolio()
                    }

                    override fun openAddFunds(rawCurrencyId: CryptoCurrency.RawID) {
                        model.openAddFunds(rawCurrencyId)
                    }
                },
            )
        } else {
            null
        }

    private val addToPortfolioSlot = childSlot(
        source = model.addToPortfolioSheetNavigation,
        serializer = AddToPortfolioSlotRoute.serializer(),
        handleBackButton = false,
        childFactory = ::addToPortfolioChild,
    )

    private val addFundsSlot = childSlot(
        source = model.addFundsSheetNavigation,
        serializer = AddFundsSlotRoute.serializer(),
        key = "addFundsSlot",
        handleBackButton = false,
        childFactory = ::addFundsChild,
    )

    init {
        componentScope.launch(dispatchers.default) {
            model.networksState.collectLatest { state ->
                when (state) {
                    is TokenNetworksState.NetworksAvailable -> {
                        portfolioBlockComponent?.setTokenNetworks(state.networks)
                        portfolioComponent?.setTokenNetworks(state.networks)
                    }
                    TokenNetworksState.NoNetworksAvailable -> {
                        portfolioBlockComponent?.setNoNetworksAvailable()
                        portfolioComponent?.setNoNetworksAvailable()
                    }
                    else -> {}
                }
            }
        }

        // === Analytics ===
        if (analyticsParams != null) {
            analyticsEventHandler.send(
                MarketDetailsAnalyticsEvent.EventBuilder(
                    token = params.token,
                ).screenOpened(
                    blockchain = analyticsParams.blockchain,
                    source = analyticsParams.source,
                    newsId = analyticsParams.newsId,
                ),
            )
        }
    }

    private fun addToPortfolioChild(
        @Suppress("UNUSED_PARAMETER") config: AddToPortfolioSlotRoute,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        return addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(addToPortfolioManager = model.addToPortfolioManager),
        )
    }

    private fun addFundsChild(
        config: AddFundsSlotRoute,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val launchMode = ManageFundsComponent.LaunchMode.FilteredByRawId(rawCurrencyId = config.rawCurrencyId)
        return manageFundsComponentFactory.create(
            context = childByContext(componentContext),
            params = ManageFundsComponent.Params(
                launchMode = launchMode,
                onDismiss = { model.addFundsSheetNavigation.dismiss() },
            ),
        )
    }

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val state by model.state.collectAsStateWithLifecycle()
        val background = LocalMainBottomSheetColor.current.value
        if (LocalRedesignEnabled.current) {
            TangemTopBar(
                startContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_back_28),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .clip(CircleShape)
                            .hazeEffectTangem { blurRadius = 8.dp }
                            .clickableSingle(
                                onClick = { params.onBackClicked() },
                                enabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                            )
                            .padding(TangemTheme.dimens2.x2),
                    )
                },
                endContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_share_new_24),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .clip(CircleShape)
                            .hazeEffectTangem { blurRadius = 8.dp }
                            .clickableSingle(
                                onClick = state.onShareClick,
                                enabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                            )
                            .padding(TangemTheme.dimens2.x2_5),
                    )
                },
                type = if (LocalIsOpenedInBottomSheet.current) {
                    TangemTopBarType.BottomSheet
                } else {
                    TangemTopBarType.Default
                },
            )
        } else {
            MarketsTokenDetailsTopBar(
                onBackClick = { params.onBackClicked() },
                isBackButtonEnabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                shouldShowPriceSubtitle = state.shouldShowPriceSubtitle,
                tokenName = state.tokenName,
                tokenPrice = state.priceText,
                backgroundColor = background,
                onShareClick = state.onShareClick,
            )
        }
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by addToPortfolioSlot.subscribeAsState()
        val addFundsBs by addFundsSlot.subscribeAsState()
        val bsState by bottomSheetState
        LaunchedEffect(bsState) {
            model.isVisibleOnScreen.value = bsState == BottomSheetState.EXPANDED
        }

        MarketsTokenDetailsContent(
            contentPadding = contentPadding,
            modifier = modifier,
            backgroundColor = LocalMainBottomSheetColor.current.value,
            state = state,
            portfolioBlock = portfolioComponent?.let { component ->
                { blockModifier ->
                    component.Content(blockModifier)
                }
            },
            portfolioFloatingBlock = portfolioBlockComponent?.let { component ->
                { blockModifier ->
                    component.Content(blockModifier)
                }
            },
        )
        bottomSheet.child?.instance?.BottomSheet()
        addFundsBs.child?.instance?.BottomSheet()
    }

    @Serializable
    data class Params(
        val token: TokenMarketParams,
        val appCurrency: AppCurrency,
        val shouldShowPortfolio: Boolean,
        val analyticsParams: AnalyticsParams?,
        val onBackClicked: () -> Unit,
        val onArticleClick: (articleId: Int, preselectedArticlesId: List<Int>) -> Unit,
        val preselectedSection: PreselectedTokenDetailsSection? = null,
        val shouldOpenExchanges: Boolean = false,
        val exchangesCount: Int? = null,
    )

    @Serializable
    data class AnalyticsParams(
        val blockchain: String?,
        val source: String,
        val newsId: Int? = null,
    )
}