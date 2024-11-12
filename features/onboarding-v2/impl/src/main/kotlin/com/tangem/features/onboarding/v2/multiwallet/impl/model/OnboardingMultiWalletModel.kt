package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.CardDTO
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.state.OnboardingMultiWalletUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class OnboardingMultiWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
) : Model() {

    private val params = paramsContainer.require<OnboardingMultiWalletComponent.Params>()
    private val currentScanResponse = MutableStateFlow(params.scanResponse)

    val state = MutableStateFlow(
        OnboardingMultiWalletState(
            currentStep = getInitialStep(),
        ),
    )

    val uiState = MutableStateFlow(
        OnboardingMultiWalletUM(
            onCreateWalletClick = { createWallet(false) },
            showSeedPhraseOption = params.withSeedPhraseFlow,
            onOtherOptionsClick = { /* navigate */ },
            onBack = { },
            dialog = null,
        ),
    )

    init {
        initScreenTitleSub()
    }

    private fun getInitialStep(): OnboardingMultiWalletState.Step {
        val card = currentScanResponse.value.card

        return when {
            card.wallets.isNotEmpty() && card.backupStatus == CardDTO.BackupStatus.NoBackup ->
                OnboardingMultiWalletState.Step.AddBackupDevice
            card.wallets.isNotEmpty() && card.backupStatus?.isActive == true ->
                OnboardingMultiWalletState.Step.FinishBackup
            else ->
                OnboardingMultiWalletState.Step.CreateWallet
        }
    }

    private fun initScreenTitleSub() {
        val title = screenTitleByStep(getInitialStep())
        params.titleProvider.changeTitle(title)

        modelScope.launch {
            state
                .map { it.currentStep }
                .collectLatest { step ->
                    val title = screenTitleByStep(step)
                    params.titleProvider.changeTitle(title)
                }
        }
    }

    private fun createWallet(shouldReset: Boolean) {
        modelScope.launch {
            val result = tangemSdkManager.createProductWallet(
                scanResponse = currentScanResponse.value,
                shouldReset = shouldReset,
            )

            when (result) {
                is CompletionResult.Success -> {
                    currentScanResponse.update {
                        it.copy(
                            card = result.data.card,
                            derivedKeys = result.data.derivedKeys,
                            primaryCard = result.data.primaryCard,
                        )
                    }

                    // TODO
                    // Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                }

                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.WalletAlreadyCreated) {
                        // show should reset dialog
                        handleActivationError()
                    }
                }
            }
        }
    }

    private fun handleActivationError() {
        uiState.update {
            it.copy(
                dialog = resetCardDialog(
                    onConfirm = {
                        uiState.update { it.copy(dialog = null) }
                        resetCard()
                    },
                    onDismiss = {
                        uiState.update { it.copy(dialog = null) }
                    },
                    onDismissButtonClick = ::navigateToSupportScreen,
                ),
            )
        }
    }

    private fun resetCard() {
        createWallet(true)
    }

    fun navigateToSupportScreen() {
        modelScope.launch {
            val cardInfo = getCardInfoUseCase(currentScanResponse.value).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(cardInfo))
        }
    }
}