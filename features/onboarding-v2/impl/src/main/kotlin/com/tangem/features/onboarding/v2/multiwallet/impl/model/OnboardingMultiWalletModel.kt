package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.interruptBackupDialog
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState.FinalizeStage
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.state.OnboardingMultiWalletUM
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class OnboardingMultiWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analyticsHandler: AnalyticsEventHandler,
    private val backupServiceHolder: BackupServiceHolder,
    private val appPreferencesStore: AppPreferencesStore,
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
        _uiState.update { st ->
            st.copy(
                dialog = interruptBackupDialog(
                    onConfirm = {
                        removeFinalizeScanResponseState()
                        router.pop()
                    },
                    dismiss = { _uiState.update { it.copy(dialog = null) } },
                ),
            )
        }
    }

    private fun removeFinalizeScanResponseState() {
        // discarding onboarding means we have to remove scan response from preferences
        // to prevent showing finalize screen dialog on next app start
        modelScope.launch {
            appPreferencesStore.editData { mutablePreferences ->
                mutablePreferences.remove(PreferencesKeys.ONBOARDING_FINALIZE_SCAN_RESPONSE_KEY)
            }
        }
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
        val backupService = backupServiceHolder.backupService.get()!!

        return when {
            // interrupted backup
            backupService.hasIncompletedBackup -> OnboardingMultiWalletState.Step.Finalize

            // Add backup button
            // Wallet1 without backup and userwallet's scanResponse doesn't contain primary card.
            card.wallets.isNotEmpty() && card.backupStatus == CardDTO.BackupStatus.NoBackup &&
                scanResponse.primaryCard == null -> OnboardingMultiWalletState.Step.ScanPrimary

            card.wallets.isNotEmpty() && card.backupStatus == CardDTO.BackupStatus.NoBackup ->
                OnboardingMultiWalletState.Step.AddBackupDevice
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