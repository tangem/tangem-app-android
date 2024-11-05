package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.model

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.scan.ProductType
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.backupCardAttestationFailedDialog
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.resetBackupCardDialog
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.state.MultiWalletBackupUM
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
class MultiWalletBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val backupServiceHolder: BackupServiceHolder,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val tangemSdkManager: TangemSdkManager,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val scanResponse
        get() = params.multiWalletState.value.currentScanResponse
    private val backupService
        get() = backupServiceHolder.backupService.get()!!

    private val state = MutableStateFlow(BackupState())

    private val _uiState = MutableStateFlow(
        when (scanResponse.productType) {
            ProductType.Wallet -> getWallet1State()
            ProductType.Wallet2,
            ProductType.Ring,
            -> MultiWalletBackupUM.Wallet2(
                isRing = scanResponse.productType == ProductType.Ring,
                backupAdded = false, // backupService.hasIncompletedBackup,
                onAddBackupClick = {},
                onFinalizeButtonClick = {},
            )
            else -> error("Type: ${scanResponse.productType.name} is not supported!")
        },
    )

    val uiState: StateFlow<MultiWalletBackupUM> = _uiState
    val onDoneFlow = MutableSharedFlow<Unit>()

    private fun getWallet1State(): MultiWalletBackupUM.Wallet1 {
        return when (backupService.currentState) {
            BackupService.State.Preparing -> {
                MultiWalletBackupUM.Wallet1(
                    title = resourceReference(R.string.onboarding_title_no_backup_cards),
                    bodyText = resourceReference(R.string.onboarding_subtitle_no_backup_cards),
                    showFinalizeButton = false,
                    addBackupButtonEnabled = true,
                    addBackupButtonLoading = false,
                    artworkNumberOfBackupCards = 0,
                    onAddBackupClick = ::startBackupWallet1,
                    onFinalizeButtonClick = {},
                    onSkipButtonClick = { onDoneFlow.tryEmit(Unit) },
                )
            }
            is BackupService.State.FinalizingBackupCard -> TODO()
            BackupService.State.FinalizingPrimaryCard -> TODO()
            BackupService.State.Finished -> TODO()
        }
    }

    private fun startBackupWallet1() {
        backupService.discardSavedBackup()
        val primaryCard = scanResponse.primaryCard

        if (primaryCard != null) {
            backupService.setPrimaryCard(primaryCard)
            addBackupCardWithService()
        } else {
            // TODO wallet 2 ??? or from main?
            // BackupAction.StartAddingPrimaryCard
        }
    }

    private fun setNumberOfBackupCardsWallet1(number: Int) {
        // set state for Wallet1 for adding backup cards and disable button if there is more than 2 backup cards
        _uiState.update { st ->
            if (st !is MultiWalletBackupUM.Wallet1) return@update st

            when (number) {
                0 -> {
                    st.copy(
                        title = resourceReference(R.string.onboarding_title_no_backup_cards),
                        bodyText = resourceReference(R.string.onboarding_subtitle_no_backup_cards),
                        artworkNumberOfBackupCards = 0,
                        addBackupButtonEnabled = true,
                    )
                }
                1 -> {
                    st.copy(
                        title = resourceReference(R.string.onboarding_title_one_backup_card),
                        bodyText = resourceReference(R.string.onboarding_subtitle_one_backup_card),
                        artworkNumberOfBackupCards = 1,
                        addBackupButtonEnabled = true,
                        showFinalizeButton = true,
                    )
                }
                2 -> {
                    st.copy(
                        title = resourceReference(R.string.onboarding_title_two_backup_cards),
                        bodyText = resourceReference(R.string.onboarding_subtitle_two_backup_cards),
                        artworkNumberOfBackupCards = 2,
                        addBackupButtonEnabled = false,
                    )
                }
                else -> st.copy(addBackupButtonEnabled = false)
            }
        }
    }

    private fun addBackupCardWithService() {
        updateWallet1State { st ->
            st.copy(addBackupButtonLoading = true)
        }

        backupService.addBackupCard { result ->
            backupService.skipCompatibilityChecks = false
            cardSdkConfigRepository.sdk.config.filter.cardIdFilter = null

            updateWallet1State { st ->
                st.copy(addBackupButtonLoading = false)
            }

            when (result) {
                is CompletionResult.Success -> {
                    // TODO change artwork
                    state.update {
                        it.copy(
                            backupCards = it.backupCards + result.data,
                            backupCardsNumber = it.backupCardsNumber + 1,
                        )
                    }
                    setNumberOfBackupCardsWallet1(state.value.backupCardsNumber)
                }
                is CompletionResult.Failure -> {
                    when (val error = result.error) {
                        is TangemSdkError.BackupFailedNotEmptyWallets -> {
                            updateWallet1State { st ->
                                st.copy(
                                    dialog = resetBackupCardDialog(
                                        onReset = { resetBackupCard(cardId = error.cardId) },
                                        onDismiss = { updateWallet1State { it.copy(dialog = null) } },
                                    ),
                                )
                            }
                        }
                        is TangemSdkError.IssuerSignatureLoadingFailed -> {
                            updateWallet1State { st ->
                                st.copy(
                                    dialog = backupCardAttestationFailedDialog(
                                        onDismiss = { updateWallet1State { it.copy(dialog = null) } },
                                    ),
                                )
                            }
                        }
                        else -> FirebaseCrashlytics.getInstance().recordException(result.error)
                    }
                }
            }
        }
    }

    private fun resetBackupCard(cardId: String) {
        modelScope.launch {
            tangemSdkManager.resetToFactorySettings(
                cardId = cardId,
                allowsRequestAccessCodeFromRepository = false,
            )
        }
    }

    private fun updateWallet1State(block: (MultiWalletBackupUM.Wallet1) -> MultiWalletBackupUM.Wallet1) {
        _uiState.update { state ->
            if (state !is MultiWalletBackupUM.Wallet1) return@update state
            block(state)
        }
    }
}
