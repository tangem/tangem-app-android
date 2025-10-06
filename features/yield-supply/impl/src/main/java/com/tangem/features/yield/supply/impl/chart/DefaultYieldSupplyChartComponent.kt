package com.tangem.features.yield.supply.impl.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
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

    internal data class Params(
        val cryptoCurrency: CryptoCurrency.Token,
        val callback: ModelCallback? = null,
    )

    internal interface ModelCallback {
        fun onStartLoading()
        fun onSuccessLoad()
        fun onLoadFail()
    }
}