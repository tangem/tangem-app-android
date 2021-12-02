package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.tap.backupService
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import org.rekotlin.Action

class OnboardingWalletReducer {
    companion object {
        fun reduce(action: Action, state: AppState): OnboardingWalletState =
            internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, appState: AppState): OnboardingWalletState {
    val state = appState.onboardingWalletState

    return when (action) {
        is BackupAction -> state.copy(backupState = BackupReducer.reduce(action, state.backupState))
        is GlobalAction.Onboarding.Start -> OnboardingWalletState(
            backupState = state.backupState.copy(canSkipBackup = action.fromHomeScreen)
        )
        is OnboardingWalletAction.GetToCreateWalletStep ->
            OnboardingWalletState(step = OnboardingWalletStep.CreateWallet)
        is OnboardingWalletAction.ProceedBackup -> state.copy(step = OnboardingWalletStep.Backup)
        is OnboardingWalletAction.SetArtworkUrl -> {
            state.copy(artwork = action.artwork)
        }
        is OnboardingWalletAction.Done -> state.copy(step = OnboardingWalletStep.Done)
        else -> state
    }
}


class BackupReducer {
    companion object {
        fun reduce(
            action: BackupAction, state: BackupState,
        ): BackupState {

            return when (action) {
                BackupAction.IntroduceBackup -> state.copy(backupStep = BackupStep.InitBackup)

                BackupAction.StartBackup -> state.copy(backupStep = BackupStep.ScanOriginCard)

                BackupAction.StartAddingBackupCards -> {
                    state.copy(backupStep = BackupStep.AddBackupCards)
                }
                BackupAction.AddBackupCard.Success -> {
                    state.copy(backupCardsNumber = state.backupCardsNumber + 1)
                }
                BackupAction.FinishAddingBackupCards -> {
                    state
                }
                BackupAction.ShowAccessCodeInfoScreen -> {
                    state.copy(backupStep = BackupStep.SetAccessCode)
                }
                BackupAction.ShowEnterAccessCodeScreen -> {
                    state.copy(backupStep = BackupStep.EnterAccessCode)
                }
                is BackupAction.SaveFirstAccessCode -> {
                    state.copy(
                        backupStep = BackupStep.ReenterAccessCode,
                        accessCode = action.accessCode
                    )
                }
                is BackupAction.SetAccessCodeError -> {
                    state.copy(
                        accessCodeError = action.error
                    )
                }
                is BackupAction.PrepareToWritePrimaryCard -> {
                    if (state.primaryCardId == null) {
                        state.copy(
                            primaryCardId = backupService.primaryCardId,
                            backupCardIds = backupService.backupCardIds,
                            backupCardsNumber = backupService.backupCardIds.size,
                            backupStep = BackupStep.WritePrimaryCard
                        )
                    } else {
                        state.copy(
                            backupStep = BackupStep.WritePrimaryCard
                        )
                    }
                }
                is BackupAction.PrepareToWriteBackupCard -> {
                    if (state.primaryCardId == null) {
                        state.copy(
                            primaryCardId = backupService.primaryCardId,
                            backupCardIds = backupService.backupCardIds,
                            backupCardsNumber = backupService.backupCardIds.size,
                            backupStep = BackupStep.WriteBackupCard(action.cardNumber)
                        )
                    } else {
                        state.copy(
                            backupStep = BackupStep.WriteBackupCard(action.cardNumber)
                        )
                    }
                }

                is BackupAction.FinishBackup -> {
                    state.copy(
                        backupStep = BackupStep.Finished
                    )
                }

                BackupAction.WritePrimaryCard -> state
                is BackupAction.WriteBackupCard -> state
                is BackupAction.SaveAccessCodeConfirmation -> state
                BackupAction.ScanPrimaryCard -> state
                BackupAction.ShowEnterAccessCodeScreen -> state
                BackupAction.ShowReenterAccessCodeScreen -> state
                BackupAction.AddBackupCard -> state
                BackupAction.DismissBackup -> state
                is BackupAction.LoadBackupCardArtwork -> state
                is BackupAction.CheckAccessCode -> state
                BackupAction.GoToShop -> state
                BackupAction.DetermineBackupStep -> state
                BackupAction.CheckForUnfinishedBackup -> state
                BackupAction.DiscardBackup -> state
                BackupAction.ResumeBackup -> state
            }

        }
    }
}