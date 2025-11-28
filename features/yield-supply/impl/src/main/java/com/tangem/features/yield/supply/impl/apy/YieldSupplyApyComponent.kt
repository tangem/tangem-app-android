package com.tangem.features.yield.supply.impl.apy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.yield.supply.impl.apy.ui.YieldSupplyApyContent
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class YieldSupplyApyComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    val loadingState: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)
    private val chartComponent = DefaultYieldSupplyChartComponent(
        appComponentContext = child("chartComponent"),
        params = DefaultYieldSupplyChartComponent.Params(
            cryptoCurrency = params.cryptoCurrency as CryptoCurrency.Token,
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
            },
        ),
    )

    override fun dismiss() {
        params.onBackClick()
    }

    @Composable
    override fun BottomSheet() {
        val state by loadingState.collectAsState()
        YieldSupplyApyContent(
            apy = stringReference("${params.apy}%"),
            isLoading = state,
            onBackClick = params.onBackClick,
            chartComponent = chartComponent,
        )
    }

    data class Params(
        val cryptoCurrency: CryptoCurrency,
        val apy: String,
        val onBackClick: () -> Unit,
    )
}