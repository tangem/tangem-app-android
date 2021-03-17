package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.SignatureCountValidator
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardType
import com.tangem.common.extensions.getType
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.tasks.isMultiwalletAllowed
import com.tangem.tap.domain.twins.isTwinCard
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckSignedHashesMiddleware {
    fun handle(action: WalletAction.CheckSignedHashes, globalState: GlobalState?) {
        when (action) {
            is WalletAction.CheckSignedHashes.CheckIfWarningNeeded -> {
                val validator = globalState?.scanNoteResponse?.walletManager as? SignatureCountValidator
                globalState?.scanNoteResponse?.card?.let { card ->
                    globalState.warningManager?.removeWarnings(WarningMessage.Origin.Local)
                    if (card.getType() != CardType.Release) addWarningMessage(WarningMessagesManager.devCardWarning())
                    if (!preferencesStorage.wasCardScannedBefore(card.cardId)) {
                        checkIfWarningNeeded(card, validator)?.let { addWarningMessage(it) }
                    }
                    updateWarningMessages()
                }

            }
            is WalletAction.CheckSignedHashes.CheckHashesCountOnline -> checkHashesCountOnline()
            is WalletAction.CheckSignedHashes.SaveCardId -> {
                val cardId = globalState?.scanNoteResponse?.card?.cardId
                cardId?.let { preferencesStorage.saveScannedCardId(it) }
            }
        }
    }

    private fun checkIfWarningNeeded(
            card: Card, signatureCountValidator: SignatureCountValidator? = null,
    ): WarningMessage? {
        if (card.isTwinCard() || card.isMultiwalletAllowed) return null

        return if (signatureCountValidator == null) {
            if (card.walletSignedHashes ?: 0 > 0) {
                WarningMessagesManager.alreadySignedHashesWarning()
            } else {
                store.dispatch(WalletAction.CheckSignedHashes.SaveCardId)
                null
            }
        } else {
            store.dispatch(WalletAction.CheckSignedHashes.NeedToCheckHashesCountOnline)
            null
        }
    }

    private fun checkHashesCountOnline() {
        if (store.state.walletState.hashesCountVerified != false) return
        if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) return

        val card = store.state.globalState.scanNoteResponse?.card
        if (card == null || preferencesStorage.wasCardScannedBefore(card.cardId)) return

        if (card.isTwinCard() || card.isMultiwalletAllowed) return

        val validator = store.state.globalState.scanNoteResponse?.walletManager
                as? SignatureCountValidator
        scope.launch {
            val result = validator?.validateSignatureCount(card.walletSignedHashes ?: 0)
            withContext(Dispatchers.Main) {
                when (result) {
                    SimpleResult.Success -> {
                        store.dispatch(WalletAction.CheckSignedHashes.ConfirmHashesCount)
                        store.dispatch(WalletAction.CheckSignedHashes.SaveCardId)
                    }
                    is SimpleResult.Failure ->
                        if (result.error is BlockchainSdkError.SignatureCountNotMatched) {
                            addWarningMessage(WarningMessagesManager.alreadySignedHashesWarning(), true)
                        } else if (card.walletSignedHashes ?: 0 > 0) {
                            addWarningMessage(WarningMessagesManager.alreadySignedHashesWarning(), true)
                        }
                }
            }
        }
    }

    private fun addWarningMessage(warning: WarningMessage, autoUpdate: Boolean = false) {
        store.state.globalState.warningManager?.addWarning(warning)
        if (autoUpdate) updateWarningMessages()
    }

    private fun updateWarningMessages() {
        val warningManager = store.state.globalState.warningManager ?: return
        store.dispatch(WalletAction.SetWarnings(warningManager.getWarnings(WarningMessage.Location.MainScreen)))
    }
}
