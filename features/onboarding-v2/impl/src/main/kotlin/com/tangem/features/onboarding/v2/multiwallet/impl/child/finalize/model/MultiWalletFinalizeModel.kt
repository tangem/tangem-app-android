package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.model

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.isRing
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.MultiWalletFinalizeComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.ui.state.MultiWalletFinalizeUM
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.resetCardDialog
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ComponentScoped
internal class MultiWalletFinalizeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val backupServiceHolder: BackupServiceHolder,
    private val tangemSdkManager: TangemSdkManager,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val multiWalletState get() = params.multiWalletState
    private val _uiState = MutableStateFlow(
        MultiWalletFinalizeUM(
            isRing = backupServiceHolder.backupService.get()?.primaryCardBatchId?.let { isRing(it) } ?: false,
            onScanClick = ::onLinkClick,
            cardNumber = backupServiceHolder.backupService.get()?.primaryCardId?.lastMasked().orEmpty(),
        ),
    )

    val uiState = _uiState.asStateFlow()
    val onEvent = MutableSharedFlow<MultiWalletFinalizeComponent.Event>()

    private fun onLinkClick() {
        when (_uiState.value.step) {
            MultiWalletFinalizeUM.Step.Primary -> {
                writePrimaryCard()
            }
            MultiWalletFinalizeUM.Step.BackupDevice1 -> {
                writeBackupCard(0)
            }
            MultiWalletFinalizeUM.Step.BackupDevice2 -> {
                writeBackupCard(1)
            }
        }
    }

    private fun writePrimaryCard() {
        val backupService = backupServiceHolder.backupService.get() ?: return
        Timber.tag("ASDASD").d(
            "writePrimaryCard ${backupService.backupCardIds} || ${backupService.currentState}",
        )
        // store.dispatchOnMain(BackupAction.SetHasRing(hasRing = isRing))

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
        Timber.tag("ASDASD").d("writeBackupCard $cardIndex || ${backupService.currentState}")
        Timber.tag("ASDASD").d("writeBackupCard ${backupService.backupCardIds}")

        // store.dispatchOnMain(BackupAction.SetHasRing(hasRing = isRing))

        val backupCardBatchId = backupService.backupCardsBatchIds.getOrNull(cardIndex) ?: return
        val isRing = isRing(backupCardBatchId)
        val iconScanRes = if (isRing) R.drawable.img_hand_scan_ring else null

        tangemSdkManager.changeProductType(isRing)
        backupService.proceedBackup(iconScanRes = iconScanRes) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val backupValidator = BackupValidator()
                    if (backupValidator.isValidBackupStatus(CardDTO(result.data)).not()) {
                        // TODO store.dispatchOnMain(BackupAction.ErrorInBackupCard)
                    }

                    if (backupService.currentState == BackupService.State.Finished) {
                        if (cardIndex == 1) {
                            modelScope.launch { onEvent.emit(MultiWalletFinalizeComponent.Event.ThreeBackupCardsAdded) }
                        } else {
                            modelScope.launch { onEvent.emit(MultiWalletFinalizeComponent.Event.TwoBackupCardsAdded) }
                        }
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

    private fun handleActivationError() {
        _uiState.update {
            it.copy(
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
        fun String.splitBySpace() = reversed().chunked(size = 4).joinToString(separator = "\u00a0").reversed()
        val last4 = takeLast(n = 4).splitBySpace()
        val mask = "\u00a0*\u00a0*\u00a0*\u00a0"
        return mask + last4
    }
}
