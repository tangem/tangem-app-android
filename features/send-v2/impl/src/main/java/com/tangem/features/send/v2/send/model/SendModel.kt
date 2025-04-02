package com.tangem.features.send.v2.send.model

import androidx.compose.runtime.Stable
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.v2.send.confirm.SendConfirmComponent
import com.tangem.features.send.v2.send.confirm.ui.state.ConfirmUM
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponent
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.properties.Delegates

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class SendModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model(),
    SendDestinationComponent.ModelCallback,
    SendAmountComponent.ModelCallback,
    SendFeeComponent.ModelCallback,
    SendConfirmComponent.ModelCallback {

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    var userWallet: UserWallet by Delegates.notNull()
    var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    var feeCryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    var appCurrency: AppCurrency = AppCurrency.Default
    var predefinedAmountValue: String? = null

    override fun onResult(sendUM: SendUM) {
        _uiState.value = sendUM
    }

    override fun onDestinationResult(destinationUM: DestinationUM) {
        _uiState.update { it.copy(destinationUM = destinationUM) }
    }

    override fun onAmountResult(amountUM: AmountState) {
        _uiState.update { it.copy(amountUM = amountUM) }
    }

    override fun onFeeResult(feeUM: FeeUM) {
        _uiState.update { it.copy(feeUM = feeUM) }
    }

    private fun initialState(): SendUM = SendUM(
        amountUM = AmountState.Empty(),
        destinationUM = DestinationUM.Empty(),
        feeUM = FeeUM.Empty(),
        confirmUM = ConfirmUM.Empty,
    )
}