package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.TangemPayChangePinComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayChangePinUM
import com.tangem.features.tangempay.model.transformers.PinCodeChangeTransformer
import com.tangem.features.tangempay.navigation.TangemPayCardDetailsInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayChangePinModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val uiMessageSender: UiMessageSender,
    private val router: Router,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val analytics: AnalyticsEventHandler,
    private val featureToggles: TangemPayFeatureToggles,
    private val fetcher: PaymentAccountStatusFetcher,
) : Model() {

    private val params: TangemPayChangePinComponent.Params = paramsContainer.require()
    val uiState: StateFlow<TangemPayChangePinUM>
        field = MutableStateFlow(getInitialState())

    init {
        analytics.send(TangemPayAnalyticsEvents.ChangePinScreenShown())
    }

    fun isRedesignEnabled(): Boolean = featureToggles.isRedesignEnabled

    private fun onPinCodeChange(pin: String) {
        uiState.update(transformer = PinCodeChangeTransformer(newPin = pin))
        val state = uiState.value
        // In the redesign there is no submit button: a valid full PIN is submitted automatically.
        if (featureToggles.isRedesignEnabled && state.submitButtonEnabled && !state.submitButtonLoading) {
            onClickSubmit()
        }
    }

    private fun onClickSubmit() {
        analytics.send(TangemPayAnalyticsEvents.ChangePinSubmitClicked())
        modelScope.launch {
            uiState.update { it.copy(submitButtonLoading = true) }
            val result = try {
                cardDetailsRepository.setPin(
                    userWalletId = params.userWalletId,
                    cardId = params.card.id,
                    pin = uiState.value.pinCode,
                ).getOrNull()
            } catch (e: Exception) {
                TangemLogger.e("Error", e)
                uiState.update { it.copy(submitButtonLoading = false) }
                uiMessageSender.send(message = ToastMessage(resourceReference(R.string.common_unknown_error)))
                return@launch
            }
            when (result) {
                SetPinResult.PIN_TOO_WEAK -> {
                    uiState.update { it.copy(submitButtonLoading = false) }
                    uiMessageSender.send(
                        message = ToastMessage(resourceReference(R.string.tangempay_pin_validation_error_message)),
                    )
                }
                SetPinResult.SUCCESS -> {
                    fetcher.invoke(params = PaymentAccountStatusFetcher.Params(userWalletId = params.userWalletId))
                    analytics.send(TangemPayAnalyticsEvents.ChangePinSuccessShown())
                    router.push(TangemPayCardDetailsInnerRoute.ChangePINSuccess)
                }
                SetPinResult.DECRYPTION_ERROR,
                SetPinResult.UNKNOWN_ERROR,
                null,
                -> {
                    uiState.update { it.copy(submitButtonLoading = false) }
                    uiMessageSender.send(message = ToastMessage(resourceReference(R.string.common_unknown_error)))
                }
            }
        }
    }

    private fun getInitialState(): TangemPayChangePinUM {
        return TangemPayChangePinUM(
            pinCode = "",
            onPinCodeChange = ::onPinCodeChange,
            onSubmitClick = ::onClickSubmit,
            submitButtonEnabled = false,
            submitButtonLoading = false,
            error = null,
        )
    }
}