package com.tangem.features.onboarding.v2.visa.impl.child.approve.model

import androidx.compose.runtime.Stable
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
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.features.onboarding.v2.visa.impl.child.approve.OnboardingVisaApproveComponent
import com.tangem.features.onboarding.v2.visa.impl.child.approve.ui.state.OnboardingVisaApproveUM
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.ONBOARDING_SOURCE
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.OnboardingVisaAnalyticsEvent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.VisaAnalyticsEvent
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class OnboardingVisaApproveModel @Inject constructor(
    paramsContainer: ParamsContainer,
    visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<OnboardingVisaApproveComponent.Config>()
    private val visaActivationRepository = visaActivationRepositoryFactory.create(
        VisaCardId(
            cardId = params.scanResponse.card.cardId,
            cardPublicKey = params.scanResponse.card.cardPublicKey.toHexString(),
        ),
    )

    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<Unit>()

    init {
        analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.WalletPrepare)
    }

    private fun getInitialState(): OnboardingVisaApproveUM {
        return OnboardingVisaApproveUM(
            onApproveClick = ::onApproveClick,
        )
    }

    private fun onApproveClick() {
        loading(true)

        analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.ButtonApprove)

        modelScope.launch {
            val dataToSign = runCatching {
                visaActivationRepository.getCustomerWalletAcceptanceData(params.preparationDataForApprove.request)
            }.getOrElse {
                loading(false)
                uiMessageSender.showErrorDialog(VisaAPIError)
                return@launch
            }

            val result = tangemSdkManager.visaCustomerWalletApprove(
                visaDataForApprove = VisaDataForApprove(
                    customerWalletCardId = params.customerWalletCardId,
                    targetAddress = params.preparationDataForApprove.customerWalletAddress,
                    dataToSign = dataToSign,
                ),
            )

            val resultData = when (result) {
                is CompletionResult.Failure -> {
                    loading(false)
                    uiMessageSender.showErrorDialog(result.error.universalError)
                    analyticsEventHandler.send(
                        VisaAnalyticsEvent.Errors(result.error.code.toString(), ONBOARDING_SOURCE),
                    )
                    return@launch
                }
                is CompletionResult.Success -> result.data
            }

            runCatching {
                visaActivationRepository.approveByCustomerWallet(resultData)
            }.onFailure {
                loading(false)
                uiMessageSender.showErrorDialog(VisaAPIError)
                return@launch
            }

            onDone.emit(Unit)
        }
    }

    private fun loading(state: Boolean) {
        _uiState.update { it.copy(approveButtonLoading = state) }
    }
}
