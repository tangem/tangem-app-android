package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.state.OnboardingMultiWalletUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class OnboardingMultiWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {
    private val params = paramsContainer.require<OnboardingMultiWalletComponent.Params>()
    private val getCardImageUseCase = GetCardImageUseCase()
    private val _uiState = MutableStateFlow(OnboardingMultiWalletUM())

    val state = MutableStateFlow(
        OnboardingMultiWalletState(
            currentStep = getInitialStep(),
            currentScanResponse = params.scanResponse,
            accessCode = null,
            isThreeCards = true,
            resultUserWallet = null,
        ),
    )
    val backups = MutableStateFlow(MultiWalletChildParams.Backup())

    val uiState = _uiState.asStateFlow()

    init {
// [REDACTED_TODO_COMMENT]
        // if (!manager.isActivationStarted(notNullCard.cardId)) {
        //     Analytics.send(Onboarding.Started())
        // }
        initScreenTitleSub()
        loadCardArtwork()
        subscribeToBackups()
    }

    private fun subscribeToBackups() {
        modelScope.launch {
            backups.collectLatest { bst ->
                when {
                    _uiState.value.artwork2Url == null && bst.card2 != null -> {
                        val artwork =
                            getCardImageUseCase.invoke(bst.card2.cardId, bst.card2.cardPublicKey)
                        _uiState.update {
                            it.copy(artwork2Url = artwork)
                        }
                    }
                    _uiState.value.artwork3Url == null && bst.card3 != null -> {
                        val artwork =
                            getCardImageUseCase.invoke(bst.card3.cardId, bst.card3.cardPublicKey)
                        _uiState.update {
                            it.copy(artwork3Url = artwork)
                        }
                    }
                }
            }
        }
    }

    private fun getInitialStep(): OnboardingMultiWalletState.Step {
        val card = params.scanResponse.card
// [REDACTED_TODO_COMMENT]
        return when {
            card.wallets.isNotEmpty() && card.backupStatus == CardDTO.BackupStatus.NoBackup ->
                OnboardingMultiWalletState.Step.AddBackupDevice
            card.wallets.isNotEmpty() && card.backupStatus?.isActive == true ->
                OnboardingMultiWalletState.Step.Finalize
            else ->
                OnboardingMultiWalletState.Step.CreateWallet
        }
    }

    private fun initScreenTitleSub() {
        val title = screenTitleByStep(getInitialStep())
        params.titleProvider.changeTitle(title)

        modelScope.launch {
            state.map { it.currentStep }
                .collectLatest { step ->
                    val stepTitle = screenTitleByStep(step)
                    params.titleProvider.changeTitle(stepTitle)
                }
        }
    }

    private fun loadCardArtwork() {
        modelScope.launch {
            val artwork =
                getCardImageUseCase.invoke(params.scanResponse.card.cardId, params.scanResponse.card.cardPublicKey)

            if (artwork != getCardImageUseCase.getDefaultFallbackUrl()) {
                _uiState.update {
                    it.copy(artwork1Url = artwork)
                }
            }
        }
    }
}
