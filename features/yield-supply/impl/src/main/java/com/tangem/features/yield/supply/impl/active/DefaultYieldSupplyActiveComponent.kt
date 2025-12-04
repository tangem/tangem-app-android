package com.tangem.features.yield.supply.impl.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.active.model.YieldSupplyActiveModel
import com.tangem.features.yield.supply.impl.active.model.YieldSupplyActiveRoute
import com.tangem.features.yield.supply.impl.active.ui.YieldSupplyActiveContent
import com.tangem.features.yield.supply.impl.active.ui.YieldSupplyActiveTitle
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import com.tangem.features.yield.supply.impl.subcomponents.approve.YieldSupplyApproveComponent
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldSupplyActiveComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: YieldSupplyActiveComponent.Params,
    private val appRouter: AppRouter,
) : YieldSupplyActiveComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyActiveModel = getOrCreateModel(params = params)

    private val chartComponent = DefaultYieldSupplyChartComponent(
        appComponentContext = child("chartComponent"),
        params = DefaultYieldSupplyChartComponent.Params(
            cryptoCurrency = model.cryptoCurrencyStatusFlow.value.currency as CryptoCurrency.Token,
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
                    ),
            )
        }

        slotState.child?.instance?.BottomSheet()
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