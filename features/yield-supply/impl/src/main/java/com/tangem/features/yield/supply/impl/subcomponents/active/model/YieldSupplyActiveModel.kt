package com.tangem.features.yield.supply.impl.subcomponents.active.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.subcomponents.active.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.subcomponents.active.entity.YieldSupplyActiveContentUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyActiveModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: YieldSupplyActiveComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow
    private val cryptoCurrency = cryptoCurrencyStatusFlow.value.currency

    val uiState: StateFlow<YieldSupplyActiveContentUM>
    field = MutableStateFlow(
        YieldSupplyActiveContentUM(
            totalEarnings = stringReference("0"),
            availableBalance = stringReference(
                cryptoCurrencyStatusFlow.value.value.amount.format {
                    crypto(cryptoCurrency = cryptoCurrencyStatusFlow.value.currency)
                },
            ),
            providerTitle = resourceReference(R.string.yield_module_provider),
            subtitle = combinedReference(
                resourceReference(
                    id = R.string.yield_module_earn_sheet_provider_description,
                    formatArgs = wrappedList(cryptoCurrency.symbol, cryptoCurrency.symbol),
                ),
                resourceReference(R.string.common_read_more),
            ),
        ),
    )
}