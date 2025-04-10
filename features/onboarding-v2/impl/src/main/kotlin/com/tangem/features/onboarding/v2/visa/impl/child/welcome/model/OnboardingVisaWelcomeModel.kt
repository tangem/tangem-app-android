package com.tangem.features.onboarding.v2.visa.impl.child.welcome.model

import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.error.ext.universalError
import com.tangem.core.ui.utils.showErrorDialog
import com.tangem.domain.visa.error.VisaAPIError
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.domain.visa.model.VisaCustomerWalletDataToSignRequest
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.OnboardingVisaWelcomeComponent.Config
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.OnboardingVisaWelcomeComponent.DoneEvent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.ONBOARDING_SOURCE
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.OnboardingVisaAnalyticsEvent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.VisaAnalyticsEvent
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
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventsHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<Config>()

    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<DoneEvent>()

    init {
        analyticsEventsHandler.send(OnboardingVisaAnalyticsEvent.ActivationScreenOpened)
    }

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
        analyticsEventsHandler.send(OnboardingVisaAnalyticsEvent.ButtonActivate)
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
                uiMessageSender.showErrorDialog(VisaAPIError)
                return@launch
            }

            val result = tangemSdkManager.activateVisaCard(
                mode = VisaCardActivationTaskMode.SignOnly(dataToSignByCardWallet = dataToSignByCardWallet),
                activationInput = params.activationInput,
            )

            val resultData = when (result) {
                is CompletionResult.Failure -> {
                    loading(false)
                    uiMessageSender.showErrorDialog(result.error.universalError)
                    analyticsEventsHandler.send(
                        VisaAnalyticsEvent.Errors(result.error.code.toString(), ONBOARDING_SOURCE),
                    )
                    return@launch
                }
                is CompletionResult.Success -> result.data
            }

            runCatching {
                visaActivationRepository.activateCard(resultData.signedActivationData)
            }.onFailure {
                loading(false)
                uiMessageSender.showErrorDialog(VisaAPIError)
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