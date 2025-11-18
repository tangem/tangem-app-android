package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.feepolicy.YieldSupplyFeePolicyComponent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningComponent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningEntryComponent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyStartEarningEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model(), YieldSupplyStartEarningComponent.ModelCallback, YieldSupplyFeePolicyComponent.ModelCallback {

    private val params = paramsContainer.require<YieldSupplyStartEarningEntryComponent.Params>()

    val uiState: StateFlow<YieldSupplyActionUM>
        field: MutableStateFlow<YieldSupplyActionUM> = MutableStateFlow(
            YieldSupplyActionUM(
                title = resourceReference(R.string.yield_module_start_earning),
                subtitle = resourceReference(
                    R.string.yield_module_start_earning_sheet_description,
                    wrappedList(params.cryptoCurrency.symbol),
                ),
                footer = resourceReference(
                    R.string.yield_module_start_earning_sheet_next_deposits_v2,
                    wrappedList(params.cryptoCurrency.symbol),
                ),
                footerLink = resourceReference(R.string.yield_module_start_earning_sheet_fee_policy),
                currencyIconState = CryptoCurrencyToIconStateConverter().convert(params.cryptoCurrency),
                yieldSupplyFeeUM = YieldSupplyFeeUM.Loading,
                isPrimaryButtonEnabled = false,
                isTransactionSending = false,
            ),
        )

    override fun onResult(yieldSupplyActionUM: YieldSupplyActionUM) {
        uiState.update { yieldSupplyActionUM }
    }

    override fun onBackClick() {
        if (!uiState.value.isTransactionSending) {
            router.pop()
        }
    }

    override fun onFeePolicyClick() {
        analyticsEventHandler.send(
            YieldSupplyAnalytics.ButtonFeePolicy(
                token = params.cryptoCurrency.symbol,
                blockchain = params.cryptoCurrency.network.name,
            ),
        )
        router.push(YieldSupplyStartEarningRoute.FeePolicy)
    }

    override fun onTransactionSent() {
        params.onDismiss(true)
    }
}