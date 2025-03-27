package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.common.ui.interruptBackupDialog
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState.FinalizeStage
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.state.OnboardingMultiWalletUM
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class OnboardingMultiWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    analyticsHandler: AnalyticsEventHandler,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val backupServiceHolder: BackupServiceHolder,
    private val onboardingRepository: OnboardingRepository,
    private val getCardImageUseCase: GetCardImageUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {
    private val params = paramsContainer.require<OnboardingMultiWalletComponent.Params>()
    private val _uiState = MutableStateFlow(OnboardingMultiWalletUM())

    val state = MutableStateFlow(
        OnboardingMultiWalletState(
            currentStep = getInitialStep(),
            currentScanResponse = params.scanResponse,
            accessCode = null,
            isThreeCards = true,
            resultUserWallet = null,
            startFromFinalize = getInitialStartFromFinalize(),
        ),
    )
    val backups = MutableStateFlow(MultiWalletChildParams.Backup())
    val uiState = _uiState.asStateFlow()

    init {
        analyticsHandler.send(OnboardingEvent.Started)
        initScreenTitle()
        loadCardArtwork()
        subscribeToBackups()
    }

    fun onBack() {
        uiMessageSender.send(
            interruptBackupDialog(
                onConfirm = {
                    modelScope.launch {
                        onboardingRepository.clearUnfinishedFinalizeOnboarding()
                        router.pop()
                    }
                },
            ),
        )
    }

    private fun subscribeToBackups() {
        modelScope.launch {
            backups.collectLatest { backup ->
                when {
                    _uiState.value.artwork2Url == null && backup.card2 != null -> {
                        val artwork =
                            getCardImageUseCase.invoke(backup.card2.cardId, backup.card2.cardPublicKey)
                        _uiState.update {
                            it.copy(artwork2Url = artwork)
                        }
                    }
                    _uiState.value.artwork3Url == null && backup.card3 != null -> {
                        val artwork =
                            getCardImageUseCase.invoke(backup.card3.cardId, backup.card3.cardPublicKey)
                        _uiState.update {
                            it.copy(artwork3Url = artwork)
                        }
                    }
                }
            }
        }
    }

    private fun getInitialStep(): OnboardingMultiWalletState.Step {
        val scanResponse = params.scanResponse
        val card = scanResponse.card

        return when {
            // Add backup button
            // Wallet1 without backup and userwallet's scanResponse doesn't contain primary card.
            card.wallets.isNotEmpty() && card.backupStatus == CardDTO.BackupStatus.NoBackup &&
                scanResponse.primaryCard == null -> OnboardingMultiWalletState.Step.ScanPrimary

            card.wallets.isNotEmpty() && card.backupStatus == CardDTO.BackupStatus.NoBackup -> {
                if (scanResponse.productType == ProductType.Wallet) {
                    OnboardingMultiWalletState.Step.ChooseBackupOption
                } else {
                    OnboardingMultiWalletState.Step.AddBackupDevice
                }
            }
            card.wallets.isNotEmpty() && card.backupStatus?.isActive == true ->
                OnboardingMultiWalletState.Step.Finalize
            else ->
                OnboardingMultiWalletState.Step.CreateWallet
        }
    }

    private fun getInitialStartFromFinalize(): FinalizeStage? {
        val backupService = backupServiceHolder.backupService.get() ?: return null
        return when (val state = backupService.currentState) {
            BackupService.State.FinalizingPrimaryCard -> FinalizeStage.ScanPrimaryCard
            is BackupService.State.FinalizingBackupCard -> if (state.index == 1) {
                FinalizeStage.ScanBackupFirstCard
            } else {
                FinalizeStage.ScanBackupSecondCard
            }
            else -> null
        }
    }

    private fun initScreenTitle() {
        val title = screenTitleByStep(getInitialStep())
        params.titleProvider.changeTitle(title)
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