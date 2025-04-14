package com.tangem.features.onboarding.v2.visa.impl.child.welcome.model

import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.domain.visa.model.VisaCustomerWalletDataToSignRequest
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.OnboardingVisaWelcomeComponent.Config
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.OnboardingVisaWelcomeComponent.DoneEvent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.ui.state.OnboardingVisaWelcomeUM
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

@ModelScoped
internal class OnboardingVisaWelcomeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val visaActivationRepositoryFactory: VisaActivationRepository.Factory,
) : Model() {

    private val params = paramsContainer.require<Config>()

    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<DoneEvent>()

    private fun getInitialState(): OnboardingVisaWelcomeUM {
        return OnboardingVisaWelcomeUM(
            mode = if (params is Config.WelcomeBack) {
                OnboardingVisaWelcomeUM.Mode.WelcomeBack
            } else {
                OnboardingVisaWelcomeUM.Mode.Hello
            },
            onContinueClick = ::onContinueClick,
        )
    }

    private fun onContinueClick() {
        if (params !is Config.WelcomeBack) {
            modelScope.launch { onDone.emit(DoneEvent.WelcomeDone) }
            return
        }

        val visaActivationRepository = visaActivationRepositoryFactory.create(
            VisaCardId(
                cardId = params.scanResponse.card.cardId,
                cardPublicKey = params.scanResponse.card.cardPublicKey.toHexString(),
            ),
        )

        loading(true)

        modelScope.launch {
            val dataToSignByCardWallet = runCatching {
                visaActivationRepository.getCardWalletAcceptanceData(params.dataToSignRequest)
            }.getOrElse {
                loading(false)
                // TODO show alert
                return@launch
            }

            val result = tangemSdkManager.activateVisaCard(
                mode = VisaCardActivationTaskMode.SignOnly(dataToSignByCardWallet = dataToSignByCardWallet),
                activationInput = params.activationInput,
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
                    DoneEvent.WelcomeBackDone(
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
                    ),
                )
            }
        }
    }

    private fun loading(state: Boolean) {
        _uiState.update { it.copy(continueButtonLoading = state) }
    }
}