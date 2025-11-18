package com.tangem.features.yield.supply.impl.subcomponents.active

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.yield.supply.impl.subcomponents.active.model.YieldSupplyActiveModel
import com.tangem.features.yield.supply.impl.subcomponents.active.ui.YieldSupplyActiveContent
import com.tangem.features.yield.supply.impl.subcomponents.active.ui.YieldSupplyActiveTitle
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.chart.DefaultYieldSupplyChartComponent
import kotlinx.coroutines.flow.StateFlow

internal class YieldSupplyActiveComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyActiveModel = getOrCreateModel(params = params)
    private val chartComponent = DefaultYieldSupplyChartComponent(
        appComponentContext = child("chartComponent"),
        params = DefaultYieldSupplyChartComponent.Params(
            cryptoCurrency = params.cryptoCurrencyStatusFlow.value.currency as CryptoCurrency.Token,
        ),
    )

    @Composable
    override fun Title() {
        YieldSupplyActiveTitle(onCloseClick = params.callback::onBackClick)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isBalanceHidden by params.isBalanceHiddenFlow.collectAsStateWithLifecycle()

        YieldSupplyActiveContent(
            state = state,
            isBalanceHidden = isBalanceHidden,
            chartComponent = chartComponent,
            onReadMoreClick = params.callback::onReadMoreClick,
            modifier = Modifier,
        )
    }

    @Composable
    override fun Footer() {
        SecondaryButton(
            text = stringResourceSafe(R.string.yield_module_disable_button),
            onClick = params.callback::onStopEarning,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val isBalanceHiddenFlow: StateFlow<Boolean>,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onBackClick()
        fun onStopEarning()
        fun onApprove()
        fun onReadMoreClick()
    }
}