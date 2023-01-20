package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.tap.backupService
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayReducer
import org.rekotlin.Action

object OnboardingWalletReducer {
    fun reduce(action: Action, state: AppState): OnboardingWalletState = internalReduce(action, state)
}

private fun internalReduce(action: Action, appState: AppState): OnboardingWalletState {
    val state = appState.onboardingWalletState

    return when (action) {
        is GlobalAction.Onboarding -> ReducerForGlobalAction.reduce(action, state)
        is OnboardingSaltPayAction -> OnboardingSaltPayReducer.reduce(action, state)
        is BackupAction -> state.copy(backupState = BackupReducer.reduce(action, state.backupState, state.isSaltPay))

        is OnboardingWalletAction.GetToCreateWalletStep -> OnboardingWalletState(
            step = OnboardingWalletStep.CreateWallet,
        )
        is OnboardingWalletAction.GetToSaltPayStep -> state.copy(
            step = OnboardingWalletStep.SaltPay,
        )
        is OnboardingWalletAction.ResumeBackup -> state.copy(step = OnboardingWalletStep.Backup)
        is OnboardingWalletAction.SetArtworkUrl -> {
            state.copy(cardArtworkUri = action.artworkUri)
        }
        is OnboardingWalletAction.Done -> state.copy(step = OnboardingWalletStep.Done)
        else -> state
    }
}

private object ReducerForGlobalAction {
    fun reduce(action: GlobalAction.Onboarding, state: OnboardingWalletState): OnboardingWalletState {
        return when (action) {
            is GlobalAction.Onboarding.Start -> {
                OnboardingWalletState(
                    backupState = state.backupState.copy(
                        maxBackupCards = if (action.scanResponse.cardTypesResolver.isSaltPay()) 1 else 2,
                        canSkipBackup =
                        if (action.scanResponse.cardTypesResolver.isSaltPay()) false else action.canSkipBackup,
                    ),
                    isSaltPay = action.scanResponse.cardTypesResolver.isSaltPay(),
                )
            }
            is GlobalAction.Onboarding.StartForUnfinishedBackup -> {
                OnboardingWalletState(
                    backupState = state.backupState.copy(
                        maxBackupCards = action.addedBackupCardsCount,
                        canSkipBackup = false,
                    ),
                    isSaltPay = false,
                )
            }
            else -> state
        }
    }
}

private object BackupReducer {
    @Suppress("ComplexMethod")
    fun reduce(action: BackupAction, state: BackupState, isSaltPay: Boolean): BackupState {
        return when (action) {
            is BackupAction.IntroduceBackup -> BackupState(
                backupStep = BackupStep.InitBackup,
                maxBackupCards = if (isSaltPay) 1 else 2,
                canSkipBackup = state.canSkipBackup,
            )
            BackupAction.StartAddingPrimaryCard -> state.copy(backupStep = BackupStep.ScanOriginCard)
            BackupAction.StartAddingBackupCards -> state.copy(backupStep = BackupStep.AddBackupCards)
            BackupAction.AddBackupCard.Success -> state.copy(backupCardsNumber = state.backupCardsNumber + 1)
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
                    backupCardIds = backupService.backupCardIds,
                    backupCardsNumber = backupService.backupCardIds.size,
                    backupStep = BackupStep.WritePrimaryCard,
                )
            } else {
                state.copy(backupStep = BackupStep.WritePrimaryCard)
            }
            is BackupAction.PrepareToWriteBackupCard -> if (state.primaryCardId == null) {
                state.copy(
                    primaryCardId = backupService.primaryCardId,
                    backupCardIds = backupService.backupCardIds,
                    backupCardsNumber = backupService.backupCardIds.size,
                    backupStep = BackupStep.WriteBackupCard(action.cardNumber),
                )
            } else {
                state.copy(backupStep = BackupStep.WriteBackupCard(action.cardNumber))
            }
            is BackupAction.FinishBackup -> state.copy(backupStep = BackupStep.Finished)
            BackupAction.OnAccessCodeDialogClosed -> state.copy(backupStep = BackupStep.AddBackupCards)
            BackupAction.DiscardBackup -> BackupState()
            else -> state
        }
    }
}
