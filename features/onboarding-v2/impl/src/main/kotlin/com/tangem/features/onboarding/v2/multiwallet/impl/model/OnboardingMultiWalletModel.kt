package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
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

    val uiState = _uiState.asStateFlow()

    init {
        // TODO add analytics
        // if (!manager.isActivationStarted(notNullCard.cardId)) {
        //     Analytics.send(Onboarding.Started())
        // }
        initScreenTitleSub()
        loadCardArtwork()
    }

    private fun getInitialStep(): OnboardingMultiWalletState.Step {
        val card = params.scanResponse.card

        // todo check local storage for scan response
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
                    it.copy(artworkUrl = artwork)
                }
            }
        }
    }
}