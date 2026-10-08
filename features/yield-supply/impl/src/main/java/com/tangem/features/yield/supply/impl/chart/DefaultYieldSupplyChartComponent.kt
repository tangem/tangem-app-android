package com.tangem.features.yield.supply.impl.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartIncentiveUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartPeriod
import com.tangem.features.yield.supply.impl.chart.model.YieldSupplyChartModel
import com.tangem.features.yield.supply.impl.chart.ui.YieldSupplyChartContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultYieldSupplyChartComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyChartModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        YieldSupplyChartContent(
            state = state,
            modifier = modifier,
        )
    }

    @AssistedFactory
    internal interface Factory {
        fun create(context: AppComponentContext, params: Params): DefaultYieldSupplyChartComponent
    }

    /**
     * @property isPlainRate keeps the chart on the Aave supply history even while a campaign is running
     * @property initialPeriod history the incentivised chart opens on; the inline chart has no period picker, so
     * whatever it is given is what it stays on
     */
    internal data class Params(
        val cryptoCurrency: CryptoCurrency.Token,
        val isPlainRate: Boolean = false,
        val initialPeriod: YieldSupplyChartPeriod = YieldSupplyChartPeriod.Week,
        val callback: ModelCallback? = null,
    )

    internal interface ModelCallback {
        fun onStartLoading()
        fun onSuccessLoad()
        fun onLoadFail()

        /** Null once the token has no running campaign, so the host sheet can fall back to its plain rate copy. */
        fun onIncentiveChanged(incentive: YieldSupplyChartIncentiveUM?)
    }
}