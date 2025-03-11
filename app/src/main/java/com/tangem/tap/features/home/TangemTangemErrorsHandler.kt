package com.tangem.tap.features.home

import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.home.errors.TangemSdkErrorHandler
import org.rekotlin.Store
import timber.log.Timber

class TangemTangemErrorsHandler(val store: Store<AppState>) : TangemSdkErrorHandler {

    override fun onErrorReceived(error: TangemError) {
        when (error) {
            is TangemSdkError -> {
                handleCardSdkError(error)
            }
            is BlockchainError -> {
                handleBlockchainSdkError(error)
            }
            else -> {
                Timber.e("Error happened", error)
            }
        }
    }

    private fun handleCardSdkError(error: TangemSdkError) {
        when (error) {
            is TangemSdkError.NfcFeatureIsUnavailable -> {
                store.dispatchOnMain(GlobalAction.ShowDialog(StateDialog.NfcFeatureIsUnavailable))
            }
            else -> {
                Timber.e(error, "Unable to scan card")
            }
        }
    }

    private fun handleBlockchainSdkError(error: TangemError) {
        Timber.e("Sdk error happened", error)
    }
}