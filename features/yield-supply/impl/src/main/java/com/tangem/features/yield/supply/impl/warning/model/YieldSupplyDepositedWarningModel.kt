package com.tangem.features.yield.supply.impl.warning.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.features.yield.supply.api.YieldSupplyDepositedWarningComponent
import com.tangem.features.yield.supply.impl.warning.ui.YieldSupplyDepositedWarningUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class YieldSupplyDepositedWarningModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params =
        paramsContainer.require<YieldSupplyDepositedWarningComponent.Params>()

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    internal val state: StateFlow<YieldSupplyDepositedWarningUM>
        field = MutableStateFlow(
            YieldSupplyDepositedWarningUM(
                iconState = iconStateConverter.convert(params.cryptoCurrency),
                onWarningAcknowledged = params.modelCallback::onYieldSupplyWarningAcknowledged,
                network = params.cryptoCurrency.name,
            ),
        )
}