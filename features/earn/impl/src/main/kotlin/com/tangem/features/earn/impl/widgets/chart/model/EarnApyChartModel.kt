package com.tangem.features.earn.impl.widgets.chart.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartComponent
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyChartUM
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class EarnApyChartModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val converter: EarnApyChartConverter,
) : Model() {

    private val params: EarnApyChartComponent.Params = paramsContainer.require()

    private val loadJobHolder = JobHolder()

    private var selectedPeriod: EarnPeriod = params.defaultPeriod

    val uiState: StateFlow<EarnApyChartUM>
        field = MutableStateFlow(loadingState(params.defaultPeriod))

    init {
        requestHistory(params.defaultPeriod)
        params.currentRate
            .onEach { rate -> uiState.update { state -> converter.updateRate(state = state, rate = rate) } }
            .launchIn(modelScope)
        params.reloads
            .onEach { reloadHistory() }
            .launchIn(modelScope)
    }

    private fun onPeriodClick(period: EarnPeriod) {
        selectedPeriod = period
        uiState.value = loadingState(period)
        requestHistory(period)
    }

    private fun onBarSelected(index: Int?) {
        uiState.update { state -> state.copy(selectedBar = index?.takeIf { it in state.bars.indices }) }
    }

    private fun requestHistory(period: EarnPeriod) {
        modelScope.launch {
            val result = params.history(period)
            ensureActive()

            uiState.value = result.fold(
                ifLeft = { error ->
                    TangemLogger.e("Earn APY history failed, period ${period.id}", error)
                    converter.failed(
                        params = params,
                        period = period,
                        onPeriodClick = ::onPeriodClick,
                        onBarSelected = ::onBarSelected,
                    )
                },
                ifRight = { history ->
                    converter.convert(
                        params = params,
                        period = period,
                        history = history,
                        onPeriodClick = ::onPeriodClick,
                        onBarSelected = ::onBarSelected,
                    )
                },
            )
        }.saveIn(loadJobHolder)
    }

    private fun reloadHistory() {
        if (uiState.value.isLoading) return

        val period = selectedPeriod

        modelScope.launch {
            val result = params.history(period)
            ensureActive()

            result
                .onLeft { error -> TangemLogger.e("Earn APY history reload failed, period ${period.id}", error) }
                .onRight { history -> showReloaded(period = period, history = history) }
        }.saveIn(loadJobHolder)
    }

    private fun showReloaded(period: EarnPeriod, history: EarnApyHistory) {
        val reloaded = converter.convert(
            params = params,
            period = period,
            history = history,
            onPeriodClick = ::onPeriodClick,
            onBarSelected = ::onBarSelected,
        )

        uiState.update { state ->
            reloaded.copy(selectedBar = state.selectedBar?.takeIf { it in reloaded.bars.indices })
        }
    }

    private fun loadingState(period: EarnPeriod): EarnApyChartUM = converter.loading(
        params = params,
        period = period,
        onPeriodClick = ::onPeriodClick,
        onBarSelected = ::onBarSelected,
    )
}