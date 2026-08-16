package com.tangem.features.tangempay.addfunds.va.deposit

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.pay.usecase.CreateVirtualAccountOrderUseCase
import com.tangem.domain.pay.usecase.GetBankCredentialsUseCase
import com.tangem.domain.pay.usecase.GetOnrampFeesUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class TangemPayVirtualAccountDepositModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val uiMessageSender: UiMessageSender,
    private val getBankCredentialsUseCase: GetBankCredentialsUseCase,
    private val createVirtualAccountOrderUseCase: CreateVirtualAccountOrderUseCase,
    private val getOnrampFeesUseCase: GetOnrampFeesUseCase,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayVirtualAccountDepositComponent.Params>()

    val uiState: StateFlow<TangemPayVirtualAccountDepositUM>
        field = MutableStateFlow(
            TangemPayVirtualAccountDepositUM(
                fees = TangemPayVirtualAccountDepositUM.FeesUM.Loading,
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

        fetchOnrampFees()
    }

    private fun fetchOnrampFees() {
        modelScope.launch {
            getOnrampFeesUseCase(userWalletId = params.userWalletId).fold(
                ifLeft = {
                    uiState.update { state ->
                        state.copy(
                            fees = TangemPayVirtualAccountDepositUM.FeesUM.Error(onRetryClick = ::onRetryFeesClick),
                        )
                    }
                },
                ifRight = { fees ->
                    val rows = fees.map { fee ->
                        val currency = getJavaCurrencyByCode(fee.currency)
                        TangemPayVirtualAccountDepositUM.FeeRow(
                            title = stringReference(fee.name),
                            value = fee.amount.format {
                                fiat(currency.currencyCode, currency.symbol).optionalDecimals()
                            },
                        )
                    }
                    uiState.update { state ->
                        state.copy(
                            fees = TangemPayVirtualAccountDepositUM.FeesUM.Content(rows.toImmutableList()),
                        )
                    }
                },
            )
        }
    }

    // Tapping the error banner swaps it for the loading shimmer, so a second tap can't reach an in-flight request.
    private fun onRetryFeesClick() {
        if (uiState.value.fees !is TangemPayVirtualAccountDepositUM.FeesUM.Error) return
        uiState.update { it.copy(fees = TangemPayVirtualAccountDepositUM.FeesUM.Loading) }
        fetchOnrampFees()
    }

    fun onDismiss() {
        params.onDismiss()
    }

    private fun onShowDetailsClick() {
        when (val onramp = params.virtualAccountOnramp) {
            is VirtualAccountOnramp.Available -> {
                analytics.send(TangemPayAnalyticsEvents.VaShowDetailsClicked())
                fetchBankCredentials(onramp)
            }
            VirtualAccountOnramp.Eligible -> {
                analytics.send(TangemPayAnalyticsEvents.VaShowDetailsFirstTimeClicked())
                createVirtualAccountOrder()
            }
            // Processing never reaches this sheet (the Preparing message is shown instead); defensive.
            VirtualAccountOnramp.Processing -> onDismiss()
        }
    }

    private fun fetchBankCredentials(onramp: VirtualAccountOnramp.Available) {
        if (uiState.value.isLoading) return
        uiState.update { it.copy(isLoading = true) }
        modelScope.launch {
            getBankCredentialsUseCase(
                userWalletId = params.userWalletId,
                productInstanceId = onramp.productInstanceId,
            ).onRight { credentials ->
                params.onShowDetails(credentials)
            }.onLeft {
                params.onShowBankingDetailsError(onramp.productInstanceId)
            }
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