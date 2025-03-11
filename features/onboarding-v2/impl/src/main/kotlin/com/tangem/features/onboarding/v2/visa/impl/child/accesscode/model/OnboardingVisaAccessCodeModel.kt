package com.tangem.features.onboarding.v2.visa.impl.child.accesscode.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.visa.model.VisaCardActivationStatus
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.domain.visa.model.VisaCustomerWalletDataToSignRequest
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.OnboardingVisaAccessCodeComponent
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.ui.state.OnboardingVisaAccessCodeUM
import com.tangem.features.onboarding.v2.visa.impl.common.ActivationReadyEvent
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.api.visa.VisaCardActivationTaskMode
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaAccessCodeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    @Suppress("UnusedPrivateMember")
    private val tangemSdkManager: TangemSdkManager,
    private val visaAuthRepository: VisaAuthRepository,
) : Model() {

    private val params: OnboardingVisaAccessCodeComponent.Config = paramsContainer.require()
    private val visaActivationRepository = visaActivationRepositoryFactory.create(
        VisaCardId(
            cardId = params.scanResponse.card.cardId,
            cardPublicKey = params.scanResponse.card.cardPublicKey.toHexString(),
        ),
    )
    private val activationStatus =
        params.scanResponse.visaCardActivationStatus as? VisaCardActivationStatus.NotStartedActivation
            ?: error("Visa activation status is not set or incorrect for this step")

    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onBack = MutableSharedFlow<Unit>()
    val onDone = MutableSharedFlow<ActivationReadyEvent>()

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
                if (checkAccessCodeMinChars().not()) return
                _uiState.update { it.copy(step = OnboardingVisaAccessCodeUM.Step.ReEnter) }
            }
            OnboardingVisaAccessCodeUM.Step.ReEnter -> {
                if (checkAccessCodesMatch().not()) return
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
            val challengeToSign = runCatching {
                visaAuthRepository.getCardAuthChallenge(
                    cardId = activationStatus.activationInput.cardId,
                    cardPublicKey = activationStatus.activationInput.cardPublicKey,
                )
            }.getOrElse {
                loading(false)
                // TODO show alert
                return@launch
            }

            val result = tangemSdkManager.activateVisaCard(
                mode = VisaCardActivationTaskMode.Full(
                    accessCode = accessCode,
                    authorizationChallenge = challengeToSign,
                ),
                activationInput = activationStatus.activationInput,
            ) as? CompletionResult.Success ?: run {
                loading(false)
                // TODO show alert
                return@launch
            }

            runCatching {
                visaActivationRepository.activateCard(result.data.signedActivationData)
            }.onFailure {
                loading(false)
                // TODO show alert
                return@launch
            }

            val targetAddress = result.data.signedActivationData.dataToSign.request.customerWalletAddress

            modelScope.launch {
                onDone.emit(
                    ActivationReadyEvent(
                        customerWalletDataToSignRequest = VisaCustomerWalletDataToSignRequest(
                            orderId = result.data.signedActivationData.dataToSign.request.orderId,
                            cardWalletAddress = result.data.signedActivationData.cardWalletAddress,
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

    private fun loading(state: Boolean) {
        _uiState.update { it.copy(buttonLoading = state) }
    }

    private companion object {
        const val ACCESS_CODE_MIN_LENGTH = 4
    }
}