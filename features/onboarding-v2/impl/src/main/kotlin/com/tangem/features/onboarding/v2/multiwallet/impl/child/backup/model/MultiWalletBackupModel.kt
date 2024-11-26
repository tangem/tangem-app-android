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
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.MultiWalletBackupComponent
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
            ProductType.Wallet,
            ProductType.Wallet2,
            ProductType.Ring,
            -> getInitState()
            else -> error("Type: ${scanResponse.productType.name} is not supported!")
        },
    )

    val uiState: StateFlow<MultiWalletBackupUM> = _uiState
    val eventFlow = MutableSharedFlow<MultiWalletBackupComponent.Event>()

    private fun getInitState(): MultiWalletBackupUM {
        return when (backupService.currentState) {
            BackupService.State.Preparing -> {
                MultiWalletBackupUM(
                    title = resourceReference(R.string.onboarding_title_no_backup_cards),
                    bodyText = resourceReference(R.string.onboarding_subtitle_no_backup_cards),
                    finalizeButtonEnabled = false,
                    addBackupButtonEnabled = true,
                    addBackupButtonLoading = false,
                    onAddBackupClick = ::startBackupWallet,
                    onFinalizeButtonClick = ::onFinalizeClick,
                    onSkipButtonClick = {
                        // eventFlow.tryEmit(Unit)
                    },
                )
            }
            is BackupService.State.FinalizingBackupCard -> TODO()
            BackupService.State.FinalizingPrimaryCard -> TODO()
            BackupService.State.Finished -> TODO()
        }
    }

    private fun startBackupWallet() {
        if (state.value.backupCardsNumber == 0) {
            backupService.discardSavedBackup()
        }
        val primaryCard = scanResponse.primaryCard

        if (primaryCard != null) {
            backupService.setPrimaryCard(primaryCard)
            addBackupCardWithService()
        } else {
            // TODO wallet 2 ??? or from main?
            // BackupAction.StartAddingPrimaryCard
        }
    }

    private fun setNumberOfBackupCards(number: Int) {
        // set state for adding backup cards and disable button if there is more than 2 backup cards
        _uiState.update { st ->
            when (number) {
                0 -> {
                    st.copy(
                        title = resourceReference(R.string.onboarding_title_no_backup_cards),
                        bodyText = resourceReference(R.string.onboarding_subtitle_no_backup_cards),
                        addBackupButtonEnabled = true,
                    )
                }
                1 -> {
                    modelScope.launch {
                        eventFlow.emit(MultiWalletBackupComponent.Event.OneDeviceAdded)
                    }
                    params.multiWalletState.update { it.copy(isThreeCards = false) }
                    st.copy(
                        title = resourceReference(R.string.onboarding_title_one_backup_card),
                        bodyText = resourceReference(R.string.onboarding_subtitle_one_backup_card),
                        addBackupButtonEnabled = true,
                        finalizeButtonEnabled = true,
                    )
                }
                2 -> {
                    modelScope.launch {
                        eventFlow.emit(MultiWalletBackupComponent.Event.TwoDeviceAdded)
                    }
                    params.multiWalletState.update { it.copy(isThreeCards = true) }
                    st.copy(
                        title = resourceReference(R.string.onboarding_title_two_backup_cards),
                        bodyText = resourceReference(R.string.onboarding_subtitle_two_backup_cards),
                        addBackupButtonEnabled = false,
                        finalizeButtonEnabled = true,
                    )
                }
                else -> st.copy(addBackupButtonEnabled = false)
            }
        }
    }

    private fun onFinalizeClick() {
        when (state.value.backupCardsNumber) {
            1 -> {
                // TODO show dialog
                params.multiWalletState.update { it.copy(isThreeCards = false) }
            }
            2 -> {
                params.multiWalletState.update { it.copy(isThreeCards = true) }
            }
        }
        modelScope.launch { eventFlow.emit(MultiWalletBackupComponent.Event.Done) }
    }

    private fun addBackupCardWithService() {
        _uiState.update { st ->
            st.copy(addBackupButtonLoading = true)
        }

        backupService.addBackupCard { result ->
            backupService.skipCompatibilityChecks = false
            cardSdkConfigRepository.sdk.config.filter.cardIdFilter = null

            _uiState.update { st ->
                st.copy(addBackupButtonLoading = false)
            }

            when (result) {
                is CompletionResult.Success -> {
                    state.update {
                        it.copy(
                            backupCardsNumber = it.backupCardsNumber + 1,
                        )
                    }
                    setNumberOfBackupCards(state.value.backupCardsNumber)
                }
                is CompletionResult.Failure -> {
                    when (val error = result.error) {
                        is TangemSdkError.BackupFailedNotEmptyWallets -> {
                            _uiState.update { st ->
                                st.copy(
                                    dialog = resetBackupCardDialog(
                                        onReset = { resetBackupCard(cardId = error.cardId) },
                                        onDismiss = { _uiState.update { it.copy(dialog = null) } },
                                    ),
                                )
                            }
                        }
                        is TangemSdkError.IssuerSignatureLoadingFailed -> {
                            _uiState.update { st ->
                                st.copy(
                                    dialog = backupCardAttestationFailedDialog(
                                        onDismiss = { _uiState.update { it.copy(dialog = null) } },
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
}