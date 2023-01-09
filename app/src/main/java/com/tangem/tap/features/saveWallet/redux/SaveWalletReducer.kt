package com.tangem.tap.features.saveWallet.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

internal object SaveWalletReducer {
    fun reduce(action: Action, state: AppState): SaveWalletState {
        return if (action is SaveWalletAction) {
            internalReduce(action, state.saveWalletState)
        } else state.saveWalletState
    }

    private fun internalReduce(action: SaveWalletAction, state: SaveWalletState): SaveWalletState {
        return when (action) {
            is SaveWalletAction.ProvideBackupInfo -> state.copy(
                backupInfo = SaveWalletState.WalletBackupInfo(
                    scanResponse = action.scanResponse,
                    accessCode = action.accessCode,
                    backupCardsIds = action.backupCardsIds,
                ),
            )
            is SaveWalletAction.Save -> state.copy(
                isSaveInProgress = true,
            )
            is SaveWalletAction.Save.Error -> state.copy(
                error = action.error,
                isSaveInProgress = false,
            )
            is SaveWalletAction.Save.Success -> state.copy(
                backupInfo = null,
                isSaveInProgress = false,
            )
            is SaveWalletAction.Dismiss -> state.copy(
                backupInfo = null,
            )
            is SaveWalletAction.CloseError -> state.copy(
                error = null,
            )
            is SaveWalletAction.EnrollBiometrics -> state.copy(
                needEnrollBiometrics = true,
            )
            is SaveWalletAction.EnrollBiometrics.Enroll,
            is SaveWalletAction.EnrollBiometrics.Cancel,
            -> state.copy(
                needEnrollBiometrics = false,
                isSaveInProgress = false,
            )
            is SaveWalletAction.SaveWalletWasShown -> state
        }
    }
}
