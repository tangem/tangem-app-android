package com.tangem.features.onboarding.v2.visa.impl.child.pincode.model

import androidx.compose.runtime.Stable
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.showErrorDialog
import com.tangem.domain.visa.SetVisaPinCodeUseCase
import com.tangem.domain.visa.error.VisaAuthorizationAPIError
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.OnboardingVisaPinCodeComponent
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.ui.state.OnboardingVisaPinCodeUM
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.OnboardingVisaAnalyticsEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class OnboardingVisaPinCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val setVisaPinCodeUseCase: SetVisaPinCodeUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<OnboardingVisaPinCodeComponent.Config>()
    private val visaCardId = VisaCardId(
        cardId = params.scanResponse.card.cardId,
        cardPublicKey = params.scanResponse.card.cardPublicKey.toHexString(),
    )
    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<Unit>()

    init {
        analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.PinCodeScreenOpened)
    }

    private fun getInitialState(): OnboardingVisaPinCodeUM {
        return OnboardingVisaPinCodeUM(
            submitButtonEnabled = false,
            onPinCodeChange = ::onPinCodeChange,
            onSubmitClick = ::onSubmitClick,
            error = if (params.wasValidationError) {
                resourceReference(R.string.visa_onboarding_pin_not_accepted)
            } else {
                null
            },
        )
    }

    private fun onPinCodeChange(pin: String) {
        if (PinCodeValidation.validateAllDigits(pin)) {
            val isError = PinCodeValidation.validateLength(pin) && PinCodeValidation.validate(pin).not()

            _uiState.update {
                it.copy(
                    pinCode = pin,
                    submitButtonEnabled = PinCodeValidation.validate(pin),
                    error = if (isError) {
                        analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.ErrorPinValidation)
                        resourceReference(R.string.visa_onboarding_pin_validation_error_message)
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun onSubmitClick() {
        analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.PinEntered)
        val pinCode = _uiState.value.pinCode
        if (PinCodeValidation.validate(pinCode).not()) return

        modelScope.launch {
            loading(true)

            setVisaPinCodeUseCase(
                pinCode = pinCode,
                visaCardId = visaCardId,
                activationOrderId = params.activationOrderInfo.orderId,
            ).onLeft {
                loading(false)
                uiMessageSender.showErrorDialog(VisaAuthorizationAPIError)
                return@launch
            }

            onDone.emit(Unit)
            loading(false)
        }
    }

    private fun loading(state: Boolean) {
        _uiState.update { it.copy(submitButtonLoading = state) }
    }
}