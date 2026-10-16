package com.tangem.features.moneyaccount.details.addfunds.options

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBankCredentialsUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class MoneyAccountAddFundsOptionsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    private val getMoneyAccountBankCredentialsUseCase: GetMoneyAccountBankCredentialsUseCase,
) : Model() {

    private val params = paramsContainer.require<MoneyAccountAddFundsOptionsComponent.Params>()

    private val converter = MoneyAccountAddFundsOptionsUMConverter(
        depositAddress = params.depositAddress,
        onSwapClick = params.listener::onSwapClick,
        onReceiveClick = params.listener::onReceiveClick,
        onBankTransferClick = ::loadBankingDetails,
    )

    private var isBankTransferLoading = false

    val uiState: StateFlow<MoneyAccountAddFundsOptionsUM>
        field = MutableStateFlow(converter.convert(MoneyAccountAddFundsOptionsUMConverter.Input()))

    init {
        analytics.send(MoneyAccountAnalyticsEvents.DepositInfoScreenOpened())
    }

    fun onDismiss() {
        params.listener.onDismissAddFunds()
    }

    private fun loadBankingDetails() {
        if (isBankTransferLoading) return
        setBankTransferLoading(isLoading = true)
        modelScope.launch {
            getMoneyAccountBankCredentialsUseCase(params.userWalletId)
                .onRight(params.listener::onBankDetailsLoaded)
                .onLeft {
                    setBankTransferLoading(isLoading = false)
                    params.listener.onBankingDetailsLoadFailed()
                }
        }
    }

    private fun setBankTransferLoading(isLoading: Boolean) {
        isBankTransferLoading = isLoading
        val input = MoneyAccountAddFundsOptionsUMConverter.Input(isBankTransferLoading = isLoading)
        uiState.value = converter.convert(input)
    }
}