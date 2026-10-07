package com.tangem.features.tangempay.addfunds

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.ReceiveAddressModel.DisplayType
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.account.isOrderRequired
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.pay.usecase.GetBankCredentialsUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayAddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    private val router: Router,
    tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val getBankCredentialsUseCase: GetBankCredentialsUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayAddFundsComponent.Params>()

    private val isVisaOnrampV2Enabled = tangemPayFeatureToggles.isVisaOnrampV2Enabled

    private val listener = object : AddFundsListener by params.listener {
        override fun onClickBankTransfer() = onBankTransferClick()
    }

    private val converter = TangemPayAddFundsUMConverter(
        listener = listener,
        virtualAccountOnramp = params.virtualAccountOnramp,
        isVisaOnrampV2Enabled = isVisaOnrampV2Enabled,
    )

    private val topUpData = TangemPayTopUpData(
        currency = params.cryptoCurrency,
        walletId = params.walletId,
        cryptoBalance = params.cryptoBalance,
        fiatBalance = params.fiatBalance,
        depositAddress = params.depositAddress,
        receiveAddress = listOf(
            ReceiveAddressModel(
                displayType = DisplayType.Default,
                value = params.depositAddress,
            ),
        ),
    )

    val uiState: StateFlow<TangemPayAddFundsUM>
        field = MutableStateFlow(converter.convert(TangemPayAddFundsUMConverter.Input(data = topUpData)))

    init {
        if (params.virtualAccountOnramp != null) {
            analytics.send(TangemPayAnalyticsEvents.VaTopupButtonShowed())
        }
    }

    private fun onBankTransferClick() {
        val onramp = params.virtualAccountOnramp
        when {
            !isVisaOnrampV2Enabled -> params.listener.onClickBankTransfer()
            onramp is VirtualAccountOnramp.Available -> loadBankingDetails(onramp)
            onramp.isOrderRequired -> openOnboarding()
        }
    }

    private fun openOnboarding() {
        analytics.send(TangemPayAnalyticsEvents.VaTopupButtonClicked())
        params.listener.onDismissAddFunds()
        router.push(TangemPayAccountDetailsInnerRoute.BankTransferOnrampOnboarding)
    }

    private fun loadBankingDetails(onramp: VirtualAccountOnramp.Available) {
        analytics.send(TangemPayAnalyticsEvents.VaTopupButtonClicked())
        setBankTransferLoading(isLoading = true)
        modelScope.launch {
            getBankCredentialsUseCase(userWalletId = params.walletId, productInstanceId = onramp.productInstanceId)
                .onRight { bankCredentials ->
                    params.listener.onDismissAddFunds()
                    router.push(TangemPayAccountDetailsInnerRoute.BankTransferOnrampDetails(bankCredentials))
                }
                .onLeft {
                    setBankTransferLoading(isLoading = false)
                    params.listener.onBankingDetailsLoadFailed(onramp.productInstanceId)
                }
        }
    }

    private fun setBankTransferLoading(isLoading: Boolean) {
        uiState.update {
            converter.convert(TangemPayAddFundsUMConverter.Input(data = topUpData, isBankTransferLoading = isLoading))
        }
    }

    fun onDismiss() {
        params.listener.onDismissAddFunds()
    }
}