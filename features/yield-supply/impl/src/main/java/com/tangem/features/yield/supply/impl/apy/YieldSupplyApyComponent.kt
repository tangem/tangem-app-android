package com.tangem.features.yield.supply.impl.apy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.yield.supply.impl.apy.ui.YieldSupplyApyContent
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartIncentiveUM
import com.tangem.features.yield.supply.impl.common.formatter.formatApy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class YieldSupplyApyComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    val loadingState: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)

    private val incentiveState: StateFlow<YieldSupplyChartIncentiveUM?>
        field = MutableStateFlow<YieldSupplyChartIncentiveUM?>(null)

    private val apyState: StateFlow<TextReference?>
        field = MutableStateFlow(params.apy?.formatApy())

    private val chartComponent = DefaultYieldSupplyChartComponent(
        appComponentContext = child("chartComponent"),
        params = DefaultYieldSupplyChartComponent.Params(
            cryptoCurrency = params.cryptoCurrency as CryptoCurrency.Token,
            isPlainRate = params.isPlainRate,
            callback = object : DefaultYieldSupplyChartComponent.ModelCallback {
                override fun onStartLoading() {
                    loadingState.update { true }
                }

                override fun onSuccessLoad() {
                    loadingState.update { false }
                }

                override fun onLoadFail() {
                    loadingState.update { false }
                }

                override fun onIncentiveChanged(incentive: YieldSupplyChartIncentiveUM?) {
                    incentiveState.update { incentive }
                }

                override fun onRateResolved(apy: TextReference) {
                    apyState.update { apy }
                }
            },
        ),
    )

    override fun dismiss() {
        params.onBackClick()
    }

    @Composable
    override fun BottomSheet() {
        val isLoadingState by loadingState.collectAsStateWithLifecycle()
        val incentive by incentiveState.collectAsStateWithLifecycle()
        val apy by apyState.collectAsStateWithLifecycle()
        YieldSupplyApyContent(
            apy = apy,
            isLoading = isLoadingState,
            incentive = incentive,
            onBackClick = params.onBackClick,
            chartComponent = chartComponent,
        )
    }

    /**
     * @property apy the rate to open on, null where only the market can say what it is — the sheet then waits for
     * the market rather than showing a rate it does not plot
     * @property isPlainRate true for the variable-rate sheet, which stays on the Aave history even on a
     * token whose campaign would otherwise take the chart over
     */
    data class Params(
        val cryptoCurrency: CryptoCurrency,
        val apy: String?,
        val isPlainRate: Boolean,
        val onBackClick: () -> Unit,
    )
}