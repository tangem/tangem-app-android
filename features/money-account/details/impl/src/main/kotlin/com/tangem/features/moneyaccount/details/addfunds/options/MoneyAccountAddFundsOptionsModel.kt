package com.tangem.features.moneyaccount.details.addfunds.options

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class MoneyAccountAddFundsOptionsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<MoneyAccountAddFundsOptionsComponent.Params>()

    private val converter = MoneyAccountAddFundsOptionsUMConverter(
        depositAddress = params.depositAddress,
        listener = params.listener,
    )

    val uiState: StateFlow<MoneyAccountAddFundsOptionsUM>
        field = MutableStateFlow(converter.convert(Unit))

    init {
        analytics.send(MoneyAccountAnalyticsEvents.DepositInfoScreenOpened())
    }

    fun onDismiss() {
        params.listener.onDismissAddFunds()
    }
}