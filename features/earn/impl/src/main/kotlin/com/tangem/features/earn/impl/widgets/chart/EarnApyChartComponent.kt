package com.tangem.features.earn.impl.widgets.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import arrow.core.Either
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.impl.widgets.chart.model.EarnApyChartModel
import com.tangem.features.earn.impl.widgets.chart.ui.EarnApyChartContent
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnPeriod
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class EarnApyChartComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: EarnApyChartModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        EarnApyChartContent(state = state, modifier = modifier)
    }

    data class Params(
        val periods: List<EarnPeriod>,
        val defaultPeriod: EarnPeriod,
        val history: suspend (EarnPeriod) -> Either<Throwable, EarnApyHistory>,
        val currentRate: EarnProductRate,
        val callback: ModelCallback? = null,
    )

    interface ModelCallback {
        fun onCalculatorClick()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnApyChartComponent
    }

    companion object {
        const val KEY = "apyChart"
    }
}