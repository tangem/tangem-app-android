package com.tangem.features.tangempay.addfunds.bankTransferOnramp.onboarding

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.CreateVirtualAccountOrderUseCase
import com.tangem.domain.pay.usecase.GetOnrampFeesUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.addfunds.VIRTUAL_ACCOUNT_TERMS_OF_USE_URL
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesLoader
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import com.tangem.features.tangempay.common.ifLoadedOrNull
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class TangemPayBankTransferOnrampOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getOnrampFeesUseCase: GetOnrampFeesUseCase,
    private val createVirtualAccountOrderUseCase: CreateVirtualAccountOrderUseCase,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankTransferOnrampOnboardingComponent.Params>()

    private val feesLoader = TangemPayOnrampFeesLoader(
        getOnrampFeesUseCase = getOnrampFeesUseCase,
        userWalletId = params.userWalletId,
        scope = modelScope,
    )

    val uiState: StateFlow<TangemPayBankTransferOnrampOnboardingScreenUM>
        field = MutableStateFlow(
            TangemPayBankTransferOnrampOnboardingScreenUM(
                fees = TangemPayOnrampFeesUM.Loading,
                isLoading = false,
                onContinueClick = ::onContinueClick,
                onTermsClick = { urlOpener.openUrl(VIRTUAL_ACCOUNT_TERMS_OF_USE_URL) },
                onCloseClick = ::onCloseClick,
            ),
        )

    init {
        analytics.send(TangemPayAnalyticsEvents.VaConditionsPopupShowedFirstTime())
        feesLoader.fees
            .onEach { fees -> uiState.update { state -> state.copy(fees = fees) } }
            .launchIn(modelScope)
        feesLoader.load()
    }

    fun onCloseClick() {
        if (uiState.value.isLoading) return
        router.pop()
    }

    private fun onContinueClick() {
        if (uiState.value.isLoading) return
        analytics.send(TangemPayAnalyticsEvents.VaShowDetailsFirstTimeClicked())
        uiState.update { state -> state.copy(isLoading = true) }
        modelScope.launch {
            val status = paymentAccountStatusSupplier(params.userWalletId).first().ifLoadedOrNull { it }
            val paymentAccountAddress = status?.paymentAccountAddress
            when {
                status == null || paymentAccountAddress == null -> onOrderFailed()
                status.virtualAccount != VirtualAccountOnramp.Eligible -> router.pop()
                else -> createOrder(paymentAccountAddress)
            }
        }
    }

    private suspend fun createOrder(paymentAccountAddress: String) {
        createVirtualAccountOrderUseCase(
            userWalletId = params.userWalletId,
            paymentAccountAddress = paymentAccountAddress,
        )
            .onRight { onOrderCreated() }
            .onLeft { onOrderFailed() }
    }

    private fun onOrderCreated() {
        analytics.send(TangemPayAnalyticsEvents.VaSuccessScreenActivation())
        router.pop()
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(R.string.tangempay_bank_transfer_request_sent),
                startIconId = CoreUiR.drawable.ic_success_20,
                isIconTinted = false,
            ),
        )
    }

    private fun onOrderFailed() {
        uiState.update { state -> state.copy(isLoading = false) }
        uiMessageSender.send(ToastMessage(resourceReference(R.string.common_unknown_error)))
    }
}