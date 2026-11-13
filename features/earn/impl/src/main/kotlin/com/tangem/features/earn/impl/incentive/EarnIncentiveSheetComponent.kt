package com.tangem.features.earn.impl.incentive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.earn.impl.incentive.ui.EarnIncentiveSheetContent
import com.tangem.features.earn.impl.incentive.ui.state.EarnIncentiveSheetUM
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow

internal class EarnIncentiveSheetComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
    chartFactory: EarnApyChartComponent.Factory,
) : ComposableBottomSheetComponent, AppComponentContext by context {

    private val chart: EarnApyChartComponent = run {
        val request = params.request
        require(request.periods.isNotEmpty()) { "The incentive sheet needs at least one period" }

        chartFactory.create(
            context = child(key = EarnApyChartComponent.KEY),
            params = EarnApyChartComponent.Params(
                periods = request.periods,
                defaultPeriod = request.periods.last(),
                history = request.history,
                currentRate = MutableStateFlow(request.rate),
                callback = null,
            ),
        )
    }

    private val state = EarnIncentiveSheetUM(
        title = EarnIncentiveSheetTexts.title,
        footerTitle = EarnIncentiveSheetTexts.poweredByTitle,
        footerDescription = EarnIncentiveSheetTexts.poweredByDescription,
        onDismiss = params.callback::onDismiss,
    )

    override fun dismiss() {
        params.callback.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        EarnIncentiveSheetContent(
            state = state,
            chart = { chart.Content(modifier = Modifier) },
        )
    }

    data class Params(
        val request: EarnIncentiveSheetRequest,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onDismiss()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnIncentiveSheetComponent
    }
}