package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import com.tangem.utils.extensions.replaceBy
import org.rekotlin.Action

@Suppress("LongMethod", "ComplexMethod")
fun globalReducer(action: Action, state: AppState): GlobalState {
    if (action !is GlobalAction) return state.globalState

    val globalState = state.globalState

    return when (action) {
        is GlobalAction.ScanFailsCounter.Increment -> {
            globalState.copy(scanCardFailsCounter = globalState.scanCardFailsCounter + 1)
        }
        is GlobalAction.ScanFailsCounter.Reset -> {
            globalState.copy(scanCardFailsCounter = 0)
        }
        is GlobalAction.SaveScanResponse -> {
            globalState.copy(scanResponse = action.scanResponse)
        }
        is GlobalAction.ChangeAppCurrency -> {
            globalState.copy(appCurrency = action.appCurrency)
        }
        is GlobalAction.RestoreAppCurrency.Success -> {
            globalState.copy(appCurrency = action.appCurrency)
        }
        is GlobalAction.UpdateWalletSignedHashes -> {
            val card = globalState.scanResponse?.card ?: return globalState
            val wallet = card.wallets
                .firstOrNull { it.publicKey.contentEquals(action.walletPublicKey) }
                ?: return globalState

            val newCardInstance = card.copy(
                wallets = card.wallets.toMutableList().also { walletsMutable ->
                    walletsMutable.replaceBy(
                        item = wallet.copy(
                            totalSignedHashes = action.walletSignedHashes,
                            remainingSignatures = action.remainingSignatures,
                        ),
                    ) { it.index == wallet.index }
                },
            )
            globalState.copy(scanResponse = globalState.scanResponse.copy(card = newCardInstance))
        }
        is GlobalAction.IsSignWithRing -> globalState.copy(isLastSignWithRing = action.isSignWithRing)
        is GlobalAction.ShowDialog -> {
            globalState.copy(dialog = action.stateDialog)
        }
        is GlobalAction.HideDialog -> {
            globalState.copy(dialog = null)
        }
        is GlobalAction.ExchangeManager.Init.Success -> {
            globalState.copy(exchangeManager = action.exchangeManager)
        }
        else -> globalState
    }
}