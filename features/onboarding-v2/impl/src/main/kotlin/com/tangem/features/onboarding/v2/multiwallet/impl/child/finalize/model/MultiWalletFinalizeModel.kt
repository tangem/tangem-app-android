package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.model

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.scan.isRing
import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.MultiWalletFinalizeComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.ui.state.MultiWalletFinalizeUM
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.resetCardDialog
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState.FinalizeStage.*
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.StringsSigns
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ComponentScoped
internal class MultiWalletFinalizeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val backupServiceHolder: BackupServiceHolder,
    private val tangemSdkManager: TangemSdkManager,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val userWalletsListManager: UserWalletsListManager,
    private val cardRepository: CardRepository,
    private val onboardingRepository: OnboardingRepository,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val multiWalletState get() = params.multiWalletState
    private val _uiState = MutableStateFlow(getInitialState())

    private val backupCardIds = backupServiceHolder.backupService.get()?.backupCardIds.orEmpty()

    private var walletHasBackupError = false
    val uiState = _uiState.asStateFlow()

    val onEvent = MutableSharedFlow<MultiWalletFinalizeComponent.Event>()

    init {
        // save scan response to preferences to be able
        // to continue finalize process after app restart
        modelScope.launch {
            onboardingRepository.saveUnfinishedFinalizeOnboarding(
                scanResponse = multiWalletState.value.currentScanResponse,
            )
        }
    }

    private fun getInitialState(): MultiWalletFinalizeUM {
        val backupService = backupServiceHolder.backupService.get() ?: return MultiWalletFinalizeUM()
        val initialStep = getInitialStep()

        val batchId = when (initialStep) {
            MultiWalletFinalizeUM.Step.Primary -> backupService.primaryCardBatchId
            MultiWalletFinalizeUM.Step.BackupDevice1 -> backupService.backupCardsBatchIds.getOrNull(0)
            MultiWalletFinalizeUM.Step.BackupDevice2 -> backupService.backupCardsBatchIds.getOrNull(1)
        }

        val cardId = when (initialStep) {
            MultiWalletFinalizeUM.Step.Primary -> backupService.primaryCardId
            MultiWalletFinalizeUM.Step.BackupDevice1 -> backupService.backupCardIds.getOrNull(0)
            MultiWalletFinalizeUM.Step.BackupDevice2 -> backupService.backupCardIds.getOrNull(1)
        }

        return MultiWalletFinalizeUM(
            isRing = batchId?.let { isRing(it) } ?: false,
            onScanClick = ::onLinkClick,
            scanPrimary = initialStep == MultiWalletFinalizeUM.Step.Primary,
            cardNumber = cardId?.lastMasked().orEmpty(),
            step = getInitialStep(),
        )
    }

    private fun getInitialStep(): MultiWalletFinalizeUM.Step {
        val startFromFinalize =
            params.multiWalletState.value.startFromFinalize ?: return MultiWalletFinalizeUM.Step.Primary

        return when (startFromFinalize) {
            ScanPrimaryCard -> MultiWalletFinalizeUM.Step.Primary
            ScanBackupFirstCard -> MultiWalletFinalizeUM.Step.BackupDevice1
            ScanBackupSecondCard -> MultiWalletFinalizeUM.Step.BackupDevice2
        }
    }

    private fun onLinkClick() {
        when (_uiState.value.step) {
            MultiWalletFinalizeUM.Step.Primary -> {
                writePrimaryCard()
            }
            MultiWalletFinalizeUM.Step.BackupDevice1 -> {
                writeBackupCard(cardIndex = 0)
            }
            MultiWalletFinalizeUM.Step.BackupDevice2 -> {
                writeBackupCard(cardIndex = 1)
            }
        }
    }

    private fun writePrimaryCard() {
        val backupService = backupServiceHolder.backupService.get() ?: return
        val primaryCardBatchId = backupService.primaryCardBatchId ?: return
        val isRing = isRing(primaryCardBatchId)
        val iconScanRes = if (isRing) R.drawable.img_hand_scan_ring else null

        tangemSdkManager.changeProductType(isRing)
        backupService.proceedBackup(iconScanRes = iconScanRes) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    modelScope.launch { onEvent.emit(MultiWalletFinalizeComponent.Event.OneBackupCardAdded) }
                    _uiState.update { st ->
                        st.copy(
                            step = MultiWalletFinalizeUM.Step.BackupDevice1,
                            isRing = backupService.backupCardsBatchIds.getOrNull(0)?.let { isRing(it) } ?: false,
                            scanPrimary = false,
                            cardNumber = backupService.backupCardIds.firstOrNull()?.lastMasked() ?: "",
                        )
                    }
                }
                is CompletionResult.Failure -> Unit
            }
            tangemSdkManager.clearProductType()
        }
    }

    private fun writeBackupCard(cardIndex: Int) {
        val backupService = backupServiceHolder.backupService.get() ?: return
        val backupCardBatchId = backupService.backupCardsBatchIds.getOrNull(cardIndex) ?: return
        val isRing = isRing(backupCardBatchId)
        val iconScanRes = if (isRing) R.drawable.img_hand_scan_ring else null

        tangemSdkManager.changeProductType(isRing)
        backupService.proceedBackup(iconScanRes = iconScanRes) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val backupValidator = BackupValidator()
                    if (backupValidator.isValidBackupStatus(CardDTO(result.data)).not()) {
                        walletHasBackupError = true
                    }

                    if (backupService.currentState == BackupService.State.Finished) {
                        finishBackup()
                    } else {
                        modelScope.launch { onEvent.emit(MultiWalletFinalizeComponent.Event.TwoBackupCardsAdded) }
                        _uiState.update { st ->
                            st.copy(
                                step = MultiWalletFinalizeUM.Step.BackupDevice2,
                                isRing = backupService.backupCardsBatchIds
                                    .getOrNull(cardIndex + 1)?.let { isRing(it) } ?: false,
                                cardNumber = backupService.backupCardIds.getOrNull(cardIndex + 1)
                                    ?.lastMasked() ?: "",
                            )
                        }
                    }
                }
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.WalletAlreadyCreated) {
                        // show should reset dialog
                        handleActivationError()
                    }
                }
            }

            tangemSdkManager.clearProductType()
        }
    }

    private fun finishBackup() {
        modelScope.launch {
            val scanResponse = params.multiWalletState.value.currentScanResponse
            val userWallet = createUserWallet(scanResponse)

            when (params.parentParams.mode) {
                OnboardingMultiWalletComponent.Mode.Onboarding -> {
                    userWalletsListManager.save(
                        userWallet = userWallet.copy(
                            scanResponse = scanResponse.updateScanResponseAfterBackup(),
                        ),
                        canOverride = true,
                    )
                }
                OnboardingMultiWalletComponent.Mode.AddBackup -> {
                    userWalletsListManager.update(
                        userWalletId = userWallet.walletId,
                        update = { wallet ->
                            wallet.copy(
                                scanResponse = scanResponse.updateScanResponseAfterBackup(),
                            )
                        },
                    )
                }
            }

            // save user wallet for manage tokens screen
            params.multiWalletState.update {
                it.copy(resultUserWallet = userWallet)
            }

            // user wallet is fully created and saved, remove scan response from preferences
            // to prevent showing finalize screen dialog on next app start
            onboardingRepository.clearUnfinishedFinalizeOnboarding()

            cardRepository.finishCardActivation(scanResponse.card.cardId)
            backupServiceHolder.backupService.get()?.discardSavedBackup()
            onEvent.emit(MultiWalletFinalizeComponent.Event.ThreeBackupCardsAdded)
        }
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse): UserWallet {
        return requireNotNull(
            value = UserWalletBuilder(scanResponse, generateWalletNameUseCase)
                .backupCardsIds(backupCardIds.toSet())
                .hasBackupError(walletHasBackupError)
                .build(),
            lazyMessage = { "User wallet not created" },
        )
    }

    @Suppress("MagicNumber")
    private fun ScanResponse.updateScanResponseAfterBackup(): ScanResponse {
        val cardsCount = if (params.multiWalletState.value.isThreeCards) 2 else 1

        val card = card.copy(
            backupStatus = CardDTO.BackupStatus.Active(cardCount = cardsCount),
            isAccessCodeSet = true,
        )
        return copy(card = card)
    }

    private fun handleActivationError() {
        _uiState.update { st ->
            st.copy(
                dialog = resetCardDialog(
                    onConfirm = ::navigateToSupportScreen,
                    dismiss = { _uiState.update { it.copy(dialog = null) } },
                    onDismissButtonClick = ::resetCard,
                ),
            )
        }
    }

    private fun resetCard() {
        val scanResponse = params.parentParams.scanResponse

        modelScope.launch {
            tangemSdkManager.resetToFactorySettings(
                cardId = scanResponse.card.cardId,
                allowsRequestAccessCodeFromRepository = true,
            )
        }
    }

    private fun navigateToSupportScreen() {
        modelScope.launch {
            val cardInfo = getCardInfoUseCase(multiWalletState.value.currentScanResponse).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(cardInfo))
        }
    }

    private fun String.lastMasked(): String {
        val space = StringsSigns.NON_BREAKING_SPACE
        fun String.splitBySpace() = reversed().chunked(size = 4).joinToString(separator = "$space").reversed()
        val last4 = takeLast(n = 4).splitBySpace()
        val mask = "$space*$space*$space*$space"
        return mask + last4
    }
}