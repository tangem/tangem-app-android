package com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.visa.model.VisaActivationRemoteState
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.OnboardingVisaOtherWalletComponent
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.ui.state.OnboardingVisaOtherWalletUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaOtherWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val visaActivationRepositoryFactory: VisaActivationRepository.Factory,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val config = paramsContainer.require<OnboardingVisaOtherWalletComponent.Config>()
    private val visaActivationRepository = visaActivationRepositoryFactory.create(config.scanResponse.card.cardId)
    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<Unit>()

    init {
        modelScope.launch {
            while (true) {
                val result = runCatching {
                    visaActivationRepository.getActivationRemoteStateLongPoll()
                }.getOrNull()

                if (result == VisaActivationRemoteState.WaitingPinCode) {
                    onDone.emit(Unit)
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
        shareManager.shareText("https://tangem.com/${config.visaDataForApprove.approveHash}")
    }

    private fun onOpenInBrowserClicked() {
        urlOpener.openUrl("https://tangem.com/${config.visaDataForApprove.approveHash}")
    }
}