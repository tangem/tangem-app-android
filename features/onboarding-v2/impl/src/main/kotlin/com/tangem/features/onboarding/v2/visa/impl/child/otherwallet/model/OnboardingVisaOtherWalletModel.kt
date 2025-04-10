package com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.model

import androidx.compose.runtime.Stable
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.visa.model.VisaActivationOrderInfo
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.model.VisaCardId
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.OnboardingVisaOtherWalletComponent
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.ui.state.OnboardingVisaOtherWalletUM
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.OnboardingVisaAnalyticsEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class OnboardingVisaOtherWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val config = paramsContainer.require<OnboardingVisaOtherWalletComponent.Config>()
    private val visaActivationRepository = visaActivationRepositoryFactory.create(
        VisaCardId(
            cardId = config.scanResponse.card.cardId,
            cardPublicKey = config.scanResponse.card.cardPublicKey.toHexString(),
        ),
    )
    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<VisaActivationOrderInfo>()

    init {
        analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.GoToWebsiteOpened)
        modelScope.launch {
            while (true) {
                val result = runCatching {
                    visaActivationRepository.getActivationRemoteStateLongPoll()
                }.getOrNull()

                if (result is VisaActivationRemoteState.AwaitingPinCode) {
                    onDone.emit(result.activationOrderInfo)
                    break
                }

                delay(timeMillis = 1000)
            }
        }
    }

    private fun getInitialState(): OnboardingVisaOtherWalletUM {
        return OnboardingVisaOtherWalletUM(
            onShareClick = ::onShareClicked,
            onOpenInBrowserClick = ::onOpenInBrowserClicked,
        )
    }

    private fun onShareClicked() {
        analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.ButtonShareLink)
        shareManager.shareText("https://tangem.com/") // TODO
    }

    private fun onOpenInBrowserClicked() {
        analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.ButtonBrowser)
        urlOpener.openUrl("https://tangem.com/") // TODO
    }
}