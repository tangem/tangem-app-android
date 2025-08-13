package com.tangem.features.onboarding.v2.note.impl.child.create.model

import com.tangem.common.CompletionResult
import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.artwork.ArtworkUM
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.note.impl.child.create.OnboardingNoteCreateWalletComponent
import com.tangem.features.onboarding.v2.note.impl.child.create.ui.state.OnboardingNoteCreateWalletUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class OnboardingNoteCreateWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val cardRepository: CardRepository,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
) : Model() {

    private val params = paramsContainer.require<OnboardingNoteCreateWalletComponent.Params>()

    private val _uiState = MutableStateFlow(
        OnboardingNoteCreateWalletUM(
            onCreateClick = ::onCreateWalletClick,
        ),
    )

    init {
        Analytics.send(OnboardingEvent.CreateWallet.ScreenOpened)
        modelScope.launch {
            val scanResponse = params.childParams.commonState.value.scanResponse ?: return@launch
            if (!cardRepository.isActivationStarted(scanResponse.card.cardId)) {
                Analytics.send(OnboardingEvent.Started)
            }
        }
        observeArtwork()
    }

    val uiState: StateFlow<OnboardingNoteCreateWalletUM> = _uiState

    private fun onCreateWalletClick() {
        modelScope.launch {
            _uiState.update {
                it.copy(createWalletInProgress = true)
            }
            val scanResponse = params.childParams.commonState.value.scanResponse ?: return@launch
            val result = tangemSdkManager.createProductWallet(scanResponse)
            when (result) {
                is CompletionResult.Success -> {
                    Analytics.send(OnboardingEvent.CreateWallet.WalletCreatedSuccessfully())
                    cardRepository.startCardActivation(scanResponse.card.cardId)
                    createWalletAndNavigateBackWithDone(scanResponse.copy(card = result.data.card))
                }
                is CompletionResult.Failure -> _uiState.update {
                    it.copy(createWalletInProgress = false)
                }
            }
        }
    }

    private fun createWalletAndNavigateBackWithDone(scanResponse: ScanResponse) {
        modelScope.launch {
            val userWallet = createUserWallet(scanResponse)
            saveWalletUseCase(userWallet, canOverride = true).onRight {
                params.onWalletCreated(userWallet)
            }
            _uiState.update {
                it.copy(createWalletInProgress = false)
            }
        }
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse): UserWallet {
        return requireNotNull(
            value = coldUserWalletBuilderFactory.create(scanResponse = scanResponse).build(),
            lazyMessage = { "User wallet not created" },
        )
    }

    private fun observeArtwork() {
        modelScope.launch {
            params.childParams.commonState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    artwork = state.cardArtwork?.let {
                        ArtworkUM(it.verifiedArtwork, it.defaultUrl)
                    },
                )
            }
        }
    }
}