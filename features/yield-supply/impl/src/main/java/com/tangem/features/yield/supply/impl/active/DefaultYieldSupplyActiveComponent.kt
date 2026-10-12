package com.tangem.features.yield.supply.impl.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.Fade
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.YieldSupplyTestTags
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.active.model.YieldSupplyActiveModel
import com.tangem.features.yield.supply.impl.active.model.YieldSupplyActiveRoute
import com.tangem.features.yield.supply.impl.active.ui.YieldSupplyActiveContent
import com.tangem.features.yield.supply.impl.active.ui.YieldSupplyActiveTitle
import com.tangem.features.yield.supply.impl.apy.about.YieldSupplyAboutApyComponent
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartPeriod
import com.tangem.features.yield.supply.impl.incentives.banner.DefaultYieldSupplyRewardsBannerComponent
import com.tangem.features.yield.supply.impl.incentives.claim.YieldSupplyClaimComponent
import com.tangem.features.yield.supply.impl.incentives.claim.result.YieldSupplyClaimResultComponent
import com.tangem.features.yield.supply.impl.subcomponents.approve.YieldSupplyApproveComponent
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldSupplyActiveComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: YieldSupplyActiveComponent.Params,
    private val appRouter: AppRouter,
    private val marketingBannerComponentFactory: MarketingBannerComponent.Factory,
) : YieldSupplyActiveComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyActiveModel = getOrCreateModel(params = params)

    private val chartComponent = DefaultYieldSupplyChartComponent(
        appComponentContext = child("chartComponent"),
        params = DefaultYieldSupplyChartComponent.Params(
            cryptoCurrency = model.cryptoCurrencyStatusFlow.value.currency as CryptoCurrency.Token,
            initialPeriod = YieldSupplyChartPeriod.ThreeMonths,
        ),
    )

    private val rewardsBannerComponent = DefaultYieldSupplyRewardsBannerComponent(
        appComponentContext = child("rewardsBannerComponent"),
        params = DefaultYieldSupplyRewardsBannerComponent.Params(
            cryptoCurrency = model.cryptoCurrencyStatusFlow.value.currency as CryptoCurrency.Token,
            cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
            onClaimClick = model::onClaimRewardsClick,
        ),
    )

    private val marketingBannerComponent = marketingBannerComponentFactory.create(
        context = child("marketingBanner"),
        params = MarketingBannerComponent.Params.Standalone(
            requestFlow = model.marketingRequest,
            onDeeplinkClick = model::onMarketingBannerDeeplink,
        ),
    )

    private val bottomSheetSlot = childSlot(
        key = "yieldSupplyActiveStack",
        source = model.slotNavigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            createChild(
                configuration,
                childByContext(
                    componentContext = factoryContext,
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isBalanceHidden by model.balanceHiddenFlow.collectAsStateWithLifecycle()
        val slotState by bottomSheetSlot.subscribeAsState()

        // The claim result draws itself full-bleed over the screen, so the slot is stacked rather than placed after.
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .background(TangemTheme.colors.background.tertiary)
                    .fillMaxSize()
                    .systemBarsPadding(),
            ) {
                YieldSupplyActiveTitle(onCloseClick = appRouter::pop)

                Box(modifier = Modifier.weight(1f)) {
                    YieldSupplyActiveContent(
                        state = state,
                        isBalanceHidden = isBalanceHidden,
                        chartComponent = chartComponent,
                        marketingBannerComponent = marketingBannerComponent,
                        rewardsBannerComponent = rewardsBannerComponent,
                        onReadMoreClick = model::onReadMoreClick,
                    )

                    Fade(
                        backgroundColor = TangemTheme.colors.background.tertiary,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                SecondaryButton(
                    text = stringResourceSafe(R.string.yield_module_stop_earning),
                    onClick = model::onStopEarning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp,
                        )
                        .testTag(YieldSupplyTestTags.STOP_EARNING_BUTTON),
                )
            }

            slotState.child?.instance?.BottomSheet()
        }
    }

    private fun createChild(
        route: YieldSupplyActiveRoute,
        factoryContext: AppComponentContext,
    ): ComposableBottomSheetComponent = when (route) {
        YieldSupplyActiveRoute.Exit -> YieldSupplyStopEarningComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyStopEarningComponent.Params(
                userWallet = model.userWallet,
                cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
                callback = model,
                wasClaimOffered = model.wasClaimOfferedBeforeWithdrawal,
            ),
        )
        is YieldSupplyActiveRoute.AboutApy -> YieldSupplyAboutApyComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyAboutApyComponent.Params(
                baseApy = route.baseApy,
                rewardsApr = route.rewardsApr,
                onDismiss = model::onAboutApyDismiss,
            ),
        )
        YieldSupplyActiveRoute.Claim -> YieldSupplyClaimComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyClaimComponent.Params(
                userWallet = model.userWallet,
                cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
                onSubmitted = model::onClaimSubmitted,
                onDismiss = model::onClaimDismiss,
            ),
        )
        YieldSupplyActiveRoute.ClaimResult -> YieldSupplyClaimResultComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyClaimResultComponent.Params(
                state = model.claimResultFlow,
                onGoToTokenClick = model::onGoToTokenClick,
                onGoToYieldClick = model::onClaimResultDismiss,
                onDismiss = model::onClaimResultDismiss,
            ),
        )
        YieldSupplyActiveRoute.Approve -> YieldSupplyApproveComponent(
            appComponentContext = factoryContext,
            params = YieldSupplyApproveComponent.Params(
                userWallet = model.userWallet,
                cryptoCurrencyStatusFlow = model.cryptoCurrencyStatusFlow,
                callback = model,
            ),
        )
    }

    @AssistedFactory
    interface Factory : YieldSupplyActiveComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: YieldSupplyActiveComponent.Params,
        ): DefaultYieldSupplyActiveComponent
    }
}