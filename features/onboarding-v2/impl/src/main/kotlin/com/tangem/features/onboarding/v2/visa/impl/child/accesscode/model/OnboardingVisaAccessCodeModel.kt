package com.tangem.features.onboarding.v2.visa.impl.child.accesscode.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import arrow.core.getOrElse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.error.UniversalError
import com.tangem.core.error.ext.universalError
import com.tangem.core.ui.utils.showErrorDialog
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.domain.visa.model.VisaCustomerWalletDataToSignRequest
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.datasource.VisaAuthRemoteDataSource
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.OnboardingVisaAccessCodeComponent
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.ui.state.OnboardingVisaAccessCodeUM
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.OnboardingVisaAnalyticsEvent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.VisaAnalyticsEvent
import com.tangem.features.onboarding.v2.visa.impl.common.ActivationReadyEvent
import com.tangem.features.onboarding.v2.visa.impl.common.unexpectedErrorAlertBS
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.api.visa.VisaCardActivationTaskMode
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class OnboardingVisaAccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    @Suppress("UnusedPrivateMember")
    private val tangemSdkManager: TangemSdkManager,
    private val visaAuthRemoteDataSource: VisaAuthRemoteDataSource,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventsHandler: AnalyticsEventHandler,
) : Model() {

    private val params: OnboardingVisaAccessCodeComponent.Config = paramsContainer.require()
    private val visaActivationRepository = visaActivationRepositoryFactory.create(
        VisaCardId(
            cardId = params.scanResponse.card.cardId,
            cardPublicKey = params.scanResponse.card.cardPublicKey.toHexString(),
        ),
    )

    private val activationInput = when (val status = params.scanResponse.visaCardActivationStatus) {
        is VisaCardActivationStatus.NotStartedActivation -> status.activationInput
        is VisaCardActivationStatus.ActivationStarted -> status.activationInput
        else -> error("Visa activation status is not set or incorrect for this step")
    }

    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onBack = MutableSharedFlow<Unit>()
    val onDone = MutableSharedFlow<ActivationReadyEvent>()

    init {
        analyticsEventsHandler.send(OnboardingVisaAnalyticsEvent.SettingAccessCodeStarted)
    }

    fun onBack() {
        if (uiState.value.buttonLoading) return

        when (uiState.value.step) {
            OnboardingVisaAccessCodeUM.Step.Enter -> modelScope.launch { onBack.emit(Unit) }
            OnboardingVisaAccessCodeUM.Step.ReEnter ->
                _uiState.update { it.copy(step = OnboardingVisaAccessCodeUM.Step.Enter) }
        }
    }

    private fun getInitialState(): OnboardingVisaAccessCodeUM {
        return OnboardingVisaAccessCodeUM(
            onAccessCodeFirstChange = ::onAccessCodeFirstChange,
            onAccessCodeSecondChange = ::onAccessCodeSecondChange,
            onAccessCodeHideClick = { _uiState.update { it.copy(accessCodeHidden = !it.accessCodeHidden) } },
            onContinue = { onContinue() },
        )
    }

    private fun onAccessCodeFirstChange(textFieldValue: TextFieldValue) {
        _uiState.update {
            it.copy(
                accessCodeFirst = textFieldValue,
                accessCodeSecond = TextFieldValue(),
                atLeastMinCharsError = false,
            )
        }
    }

    private fun onAccessCodeSecondChange(textFieldValue: TextFieldValue) {
        _uiState.update {
            it.copy(
                accessCodeSecond = textFieldValue,
                codesNotMatchError = false,
            )
        }
    }

    private fun onContinue() {
        when (uiState.value.step) {
            OnboardingVisaAccessCodeUM.Step.Enter -> {
                analyticsEventsHandler.send(OnboardingVisaAnalyticsEvent.AccessCodeEntered)
                if (checkAccessCodeMinChars().not()) return
                _uiState.update { it.copy(step = OnboardingVisaAccessCodeUM.Step.ReEnter) }
                analyticsEventsHandler.send(OnboardingVisaAnalyticsEvent.AccessCodeReenterScreen)
            }
            OnboardingVisaAccessCodeUM.Step.ReEnter -> {
                if (checkAccessCodesMatch().not()) return
                analyticsEventsHandler.send(OnboardingVisaAnalyticsEvent.OnboardingVisa)
                startActivationProcess(accessCode = uiState.value.accessCodeFirst.text)
            }
        }
    }

    private fun checkAccessCodeMinChars(): Boolean {
        val first = uiState.value.accessCodeFirst.text

        if (first.length < ACCESS_CODE_MIN_LENGTH) {
            _uiState.update { it.copy(atLeastMinCharsError = true) }
            return false
        }

        return true
    }

    private fun checkAccessCodesMatch(): Boolean {
        val first = uiState.value.accessCodeFirst.text
        val second = uiState.value.accessCodeSecond.text

        if (first != second) {
            _uiState.update { it.copy(codesNotMatchError = true) }
            return false
        }

        return true
    }

    @Suppress("UnusedPrivateMember")
    private fun startActivationProcess(accessCode: String) {
        loading(true)

        modelScope.launch {
            val challengeToSign = visaAuthRemoteDataSource.getCardAuthChallenge(
                cardId = activationInput.cardId,
                cardPublicKey = activationInput.cardPublicKey,
            ).getOrElse {
                onError(it)
                return@launch
            }

            val result = tangemSdkManager.activateVisaCard(
                mode = VisaCardActivationTaskMode.Full(
                    accessCode = accessCode,
                    authorizationChallenge = challengeToSign,
                ),
                activationInput = activationInput,
            )

            val resultData = when (result) {
                is CompletionResult.Failure -> {
                    onError(result.error.universalError)
                    return@launch
                }
                is CompletionResult.Success -> result.data
            }

            visaActivationRepository.activateCard(resultData.signedActivationData)
                .onLeft {
                    onError(it)
                    return@launch
                }

            val targetAddress =
                result.data.signedActivationData.dataToSign.request.activationOrderInfo.customerWalletAddress

            modelScope.launch {
                onDone.emit(
                    ActivationReadyEvent(
                        customerWalletDataToSignRequest = VisaCustomerWalletDataToSignRequest(
                            orderId = result.data.signedActivationData.dataToSign.request.activationOrderInfo.orderId,
                            cardWalletAddress = result.data.signedActivationData.dataToSign.request.cardWalletAddress,
                            customerWalletAddress = targetAddress,
                        ),
                        newScanResponse = params.scanResponse.copy(
                            card = result.data.newCardDTO,
                        ),
                        customerWalletTargetAddress = targetAddress,
                    ),
                )
            }
        }
    }

    private fun onError(error: UniversalError) {
        loading(false)
        if (error is VisaApiError && error.isUnknown()) {
            uiMessageSender.send(unexpectedErrorAlertBS)
        } else {
            uiMessageSender.showErrorDialog(error)
        }
        analyticsEventsHandler.send(VisaAnalyticsEvent.ErrorOnboarding(error))
    }

    private fun loading(state: Boolean) {
        _uiState.update { it.copy(buttonLoading = state) }
    }

    private companion object {
        const val ACCESS_CODE_MIN_LENGTH = 4
    }
}