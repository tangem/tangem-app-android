package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.pay.usecase.CreateVirtualAccountOrderUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.components.TangemPayVirtualAccountDepositComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayVirtualAccountDepositUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayVirtualAccountDepositModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val uiMessageSender: UiMessageSender,
    private val createVirtualAccountOrderUseCase: CreateVirtualAccountOrderUseCase,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayVirtualAccountDepositComponent.Params>()

    val uiState: StateFlow<TangemPayVirtualAccountDepositUM>
        field = MutableStateFlow(
            TangemPayVirtualAccountDepositUM(
                fees = persistentListOf(
                    TangemPayVirtualAccountDepositUM.FeeRow(
                        title = resourceReference(R.string.tangempay_bank_transfer_fee_ach),
                        value = "$1",
                    ),
                    TangemPayVirtualAccountDepositUM.FeeRow(
                        title = resourceReference(R.string.tangempay_bank_transfer_fee_fedwire),
                        value = "$11",
                    ),
                ),
                shouldShowTermsAndConditions = params.virtualAccountOnramp is VirtualAccountOnramp.Eligible,
                isLoading = false,
                onShowDetailsClick = ::onShowDetailsClick,
                onDismiss = ::onDismiss,
                onTermsClick = { urlOpener.openUrl(TERMS_OF_USE_URL) },
                onPrivacyClick = { urlOpener.openUrl(PRIVACY_POLICY_URL) },
            ),
        )

    init {
        val event = if (params.virtualAccountOnramp is VirtualAccountOnramp.Eligible) {
            TangemPayAnalyticsEvents.VaConditionsPopupShowedFirstTime()
        } else {
            TangemPayAnalyticsEvents.VaConditionsPopupShowed()
        }
        analytics.send(event)
    }

    fun onDismiss() {
        params.onDismiss()
    }

    private fun onShowDetailsClick() {
        when (params.virtualAccountOnramp) {
            is VirtualAccountOnramp.Available -> {
                analytics.send(TangemPayAnalyticsEvents.VaShowDetailsClicked())
                params.onShowDetails(params.virtualAccountOnramp)
            }
            VirtualAccountOnramp.Eligible -> {
                analytics.send(TangemPayAnalyticsEvents.VaShowDetailsFirstTimeClicked())
                createVirtualAccountOrder()
            }
            VirtualAccountOnramp.BankCredentialsError -> params.onShowBankingDetailsError()
            // Processing never reaches this sheet (the Preparing message is shown instead); defensive.
            VirtualAccountOnramp.Processing -> onDismiss()
        }
    }

    private fun createVirtualAccountOrder() {
        if (uiState.value.isLoading) return
        uiState.update { it.copy(isLoading = true) }
        modelScope.launch {
            createVirtualAccountOrderUseCase(
                userWalletId = params.userWalletId,
                paymentAccountAddress = params.paymentAccountAddress,
            ).fold(
                ifLeft = {
                    uiState.update { state -> state.copy(isLoading = false) }
                    uiMessageSender.send(ToastMessage(resourceReference(R.string.common_unknown_error)))
                },
                ifRight = {
                    uiState.update { state -> state.copy(isLoading = false) }
                    params.onOrderCreated()
                },
            )
        }
    }

    private companion object {
        const val TERMS_OF_USE_URL = "https://tangem.com/docs/en/virtual-account-terms.pdf"
        const val PRIVACY_POLICY_URL = "https://tangem.com/docs/en/pay-privacy-policy.pdf"
    }
}