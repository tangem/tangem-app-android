package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.domain.redux.OnboardingManageTokensAction
import com.tangem.tap.backupService
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import org.rekotlin.Action

object OnboardingWalletReducer {
    fun reduce(action: Action, state: AppState): OnboardingWalletState = internalReduce(action, state)
}

@Suppress("CyclomaticComplexMethod")
private fun internalReduce(action: Action, appState: AppState): OnboardingWalletState {
    val state = appState.onboardingWalletState
    val backupState = state.backupState

    return when (action) {
        is GlobalAction.Onboarding -> ReducerForGlobalAction.reduce(action, state)
        is BackupAction.BackupFinished -> {
            state.copy(
                step = if (action.userWalletId != null && backupState.startedSource == BackupStartedSource.Onboarding) {
                    OnboardingWalletStep.ManageTokens
                } else {
                    OnboardingWalletStep.Done
                },
                userWalletId = action.userWalletId,
            )
        }
        is BackupAction -> state.copy(backupState = BackupReducer.reduce(action = action, state = state.backupState))
        is OnboardingWallet2Action -> OnboardingWallet2Reducer.reduce(action, state)
        is OnboardingWalletAction.GetToCreateWalletStep -> state.copy(
            step = OnboardingWalletStep.CreateWallet,
        )
        is OnboardingWalletAction.ResumeBackup -> state.copy(step = OnboardingWalletStep.Backup)
        is OnboardingWalletAction.SetPrimaryCardArtworkUrl -> {
            state.copy(
                walletImages = state.walletImages.copy(primaryCardImage = action.artworkUri),
            )
        }
        is OnboardingWalletAction.SetSecondCardArtworkUrl -> {
            state.copy(
                walletImages = state.walletImages.copy(secondCardImage = action.artworkUri),
            )
        }
        is OnboardingWalletAction.SetThirdCardArtworkUrl -> {
            state.copy(
                walletImages = state.walletImages.copy(thirdCardImage = action.artworkUri),
            )
        }
        is OnboardingManageTokensAction.CurrenciesSaved -> state.copy(step = OnboardingWalletStep.Done)
        is OnboardingWalletAction.WalletSaved -> state.copy(
            step = when (backupState.startedSource) {
                BackupStartedSource.Onboarding -> OnboardingWalletStep.ManageTokens
                BackupStartedSource.CreateBackup -> OnboardingWalletStep.Done
            },
            userWalletId = action.userWalletId,
        )
        is OnboardingWalletAction.Done -> state.copy(step = OnboardingWalletStep.Done)
        else -> state
    }
}

private object ReducerForGlobalAction {

    private const val MAX_BACKUP_CARDS = 2

    fun reduce(action: GlobalAction.Onboarding, state: OnboardingWalletState): OnboardingWalletState {
        return when (action) {
            is GlobalAction.Onboarding.Start -> {
                OnboardingWalletState(
                    backupState = state.backupState.copy(
                        maxBackupCards = MAX_BACKUP_CARDS,
                        canSkipBackup = action.canSkipBackup,
                        startedSource = action.source,
                    ),
                )
            }
            is GlobalAction.Onboarding.StartForUnfinishedBackup -> {
                OnboardingWalletState(
                    backupState = state.backupState.copy(
                        maxBackupCards = action.addedBackupCardsCount,
                        canSkipBackup = false,
                        isInterruptedBackup = true,
                    ),
                )
            }
            else -> state
        }
    }
}

private object OnboardingWallet2Reducer {
    fun reduce(action: OnboardingWallet2Action, state: OnboardingWalletState): OnboardingWalletState = when (action) {
        is OnboardingWallet2Action.SetDependencies -> state.copy(
            wallet2State = OnboardingWallet2State(
                maxProgress = action.maxProgress,
            ),
        )
        else -> state
    }
}

private object BackupReducer {
    @Suppress("ComplexMethod", "LongMethod")
    fun reduce(action: BackupAction, state: BackupState): BackupState {
        return when (action) {
            is BackupAction.IntroduceBackup -> BackupState(
                backupStep = BackupStep.InitBackup,
                maxBackupCards = 2,
                canSkipBackup = state.canSkipBackup,
                startedSource = state.startedSource,
            )
            BackupAction.StartAddingPrimaryCard -> {
                state.copy(
                    backupStep = BackupStep.ScanOriginCard,
                    maxBackupCards = 2,
                    canSkipBackup = state.canSkipBackup,
                )
            }
            BackupAction.StartAddingBackupCards -> {
                state.copy(
                    backupStep = BackupStep.AddBackupCards,
                    maxBackupCards = 2,
                    canSkipBackup = state.canSkipBackup,
                )
            }
            is BackupAction.AddBackupCard.Success -> {
                state.copy(
                    backupCards = state.backupCards + action.card,
                    backupCardsNumber = state.backupCardsNumber + 1,
                )
            }
            is BackupAction.AddBackupCard.ChangeButtonLoading -> state.copy(showBtnLoading = action.isLoading)
            BackupAction.ShowAccessCodeInfoScreen -> state.copy(backupStep = BackupStep.SetAccessCode)
            BackupAction.ShowEnterAccessCodeScreen -> state.copy(
                backupStep = BackupStep.EnterAccessCode,
                accessCodeError = null,
            )
            is BackupAction.SaveFirstAccessCode -> state.copy(
                backupStep = BackupStep.ReenterAccessCode,
                accessCode = action.accessCode,
            )
            is BackupAction.SetAccessCodeError -> state.copy(accessCodeError = action.error)
            is BackupAction.PrepareToWritePrimaryCard -> if (state.primaryCardId == null) {
                state.copy(
                    primaryCardId = backupService.primaryCardId,
                    primaryCardBatchId = backupService.primaryCardBatchId,
                    backupCardIds = backupService.backupCardIds,
                    backupCardBatchIds = backupService.backupCardsBatchIds,
                    backupCardsNumber = backupService.backupCardIds.size,
                    backupStep = BackupStep.WritePrimaryCard,
                )
            } else {
                state.copy(backupStep = BackupStep.WritePrimaryCard)
            }
            is BackupAction.PrepareToWriteBackupCard -> if (state.primaryCardId == null) {
                state.copy(
                    primaryCardId = backupService.primaryCardId,
                    primaryCardBatchId = backupService.primaryCardBatchId,
                    backupCardIds = backupService.backupCardIds,
                    backupCardBatchIds = backupService.backupCardsBatchIds,
                    backupCardsNumber = backupService.backupCardIds.size,
                    backupStep = BackupStep.WriteBackupCard(action.cardNumber),
                )
            } else {
                state.copy(backupStep = BackupStep.WriteBackupCard(action.cardNumber))
            }
            is BackupAction.ErrorInBackupCard -> state.copy(hasBackupError = true)
            is BackupAction.SkipBackup -> state.copy(backupStep = BackupStep.Finished)
            is BackupAction.FinishBackup -> state.copy(backupStep = BackupStep.Finished)
            BackupAction.OnAccessCodeDialogClosed -> state.copy(backupStep = BackupStep.AddBackupCards)
            BackupAction.DiscardBackup -> BackupState()
            is BackupAction.SetHasRing -> {
                // Don't update if state.has ring is already true
                if (state.hasRing) {
                    state
                } else {
                    state.copy(hasRing = action.hasRing)
                }
            }
            else -> state
        }
    }
}