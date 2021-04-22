package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.SignatureCountValidator
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.common.card.Card
import com.tangem.commands.common.card.CardType
import com.tangem.commands.verification.VerifyCardState
import com.tangem.common.extensions.getType
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.extensions.hasSignedHashes
import com.tangem.tap.domain.isMultiwalletAllowed
import com.tangem.tap.domain.twins.isTwinCard
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class WarningsMiddleware {
    fun handle(action: WalletAction.Warnings, globalState: GlobalState?) {
        when (action) {
            WalletAction.Warnings.Update -> setWarningMessages()
            is WalletAction.Warnings.CheckIfNeeded -> {
                showCardWarningsIfNeeded(globalState)
                val readyToShow = preferencesStorage.appRatingLaunchObserver.isReadyToShow()
                if (readyToShow) addWarningMessage(WarningMessagesManager.appRatingWarning(), true)
            }
            is WalletAction.Warnings.CheckHashesCount.CheckHashesCountOnline -> checkHashesCountOnline()
            is WalletAction.Warnings.CheckHashesCount.SaveCardId -> {
                val cardId = globalState?.scanNoteResponse?.card?.cardId
                cardId?.let { preferencesStorage.saveScannedCardId(it) }
            }
            is WalletAction.Warnings.AppRating.RemindLater -> {
                preferencesStorage.appRatingLaunchObserver.applyDelayedShowing()
            }
            is WalletAction.Warnings.AppRating.SetNeverToShow -> {
                preferencesStorage.appRatingLaunchObserver.setNeverToShow()
            }
        }
    }

    fun tryToShowAppRatingWarning(wallet: Wallet) {
        val nonZeroWalletsCount = wallet.amounts.filter {
            it.value.value?.isGreaterThan(BigDecimal.ZERO) ?: false
        }.size
        if (nonZeroWalletsCount > 0) {
            preferencesStorage.appRatingLaunchObserver.foundWalletWithFunds()
        }
        if (preferencesStorage.appRatingLaunchObserver.isReadyToShow()) {
            FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.APP_RATING_DISPLAYED)
            addWarningMessage(WarningMessagesManager.appRatingWarning(), true)
        }
    }

    private fun showCardWarningsIfNeeded(globalState: GlobalState?) {
        globalState?.scanNoteResponse?.card?.let { card ->
            globalState.warningManager?.removeWarnings(WarningMessage.Origin.Local)
            if (card.getType() != CardType.Release) {
                addWarningMessage(WarningMessagesManager.devCardWarning())
            } else if (!preferencesStorage.wasCardScannedBefore(card.cardId)) {
                checkIfWarningNeeded(card)?.let { warning -> addWarningMessage(warning) }
            }
            if (card.getType() == CardType.Release) {
                if (globalState.scanNoteResponse.verifyResponse?.verificationState ==
                        VerifyCardState.VerifiedOffline) {
                    addWarningMessage(WarningMessagesManager.onlineVerificationFailed())
                }
            }
            setWarningMessages()
        }
    }

    private fun checkIfWarningNeeded(
            card: Card
    ): WarningMessage? {
        if (card.isTwinCard()) return null

        val validator = store.state.walletState.walletManagers.firstOrNull()
                as? SignatureCountValidator
        return if (validator == null || card.isMultiwalletAllowed) {
            if (card.hasSignedHashes()) {
                WarningMessagesManager.alreadySignedHashesWarning()
            } else {
                store.dispatch(WalletAction.Warnings.CheckHashesCount.SaveCardId)
                null
            }
        } else {
            store.dispatch(WalletAction.Warnings.CheckHashesCount.NeedToCheckHashesCountOnline)
            null
        }
    }

    private fun checkHashesCountOnline() {
        if (store.state.walletState.hashesCountVerified != false) return
        if (!NetworkConnectivity.getInstance().isOnlineOrConnecting()) return

        val card = store.state.globalState.scanNoteResponse?.card
        if (card == null || preferencesStorage.wasCardScannedBefore(card.cardId)) return

        if (card.isTwinCard() || card.isMultiwalletAllowed) return

        val validator = store.state.walletState.walletManagers.firstOrNull()
                as? SignatureCountValidator
        scope.launch {
            val signedHashes = card.getSingleWallet()?.signedHashes ?: 0
            val result = validator?.validateSignatureCount(signedHashes)
            withContext(Dispatchers.Main) {
                when (result) {
                    SimpleResult.Success -> {
                        store.dispatch(WalletAction.Warnings.CheckHashesCount.ConfirmHashesCount)
                        store.dispatch(WalletAction.Warnings.CheckHashesCount.SaveCardId)
                    }
                    is SimpleResult.Failure ->
                        if (result.error is BlockchainSdkError.SignatureCountNotMatched) {
                            addWarningMessage(WarningMessagesManager.alreadySignedHashesWarning(), true)
                        } else if (signedHashes > 0) {
                            addWarningMessage(WarningMessagesManager.alreadySignedHashesWarning(), true)
                        }
                }
            }
        }
    }

    private fun addWarningMessage(warning: WarningMessage, autoUpdate: Boolean = false) {
        store.state.globalState.warningManager?.addWarning(warning)
        if (autoUpdate) setWarningMessages()
    }

    private fun setWarningMessages() {
        store.dispatch(WalletAction.Warnings.Set(getWarnings()))
    }

    private fun getWarnings(): List<WarningMessage> {
        val warningManager = store.state.globalState.warningManager ?: return emptyList()
        return warningManager.getWarnings(WarningMessage.Location.MainScreen, store.state.walletState.blockchains)
    }
}