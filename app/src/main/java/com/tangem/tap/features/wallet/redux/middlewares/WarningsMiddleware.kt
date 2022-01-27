package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.SignatureCountValidator
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.extensions.getSingleWallet
import com.tangem.tap.domain.extensions.hasSignedHashes
import com.tangem.tap.domain.extensions.remainingSignatures
import com.tangem.tap.domain.isMultiwalletAllowed
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.network.NetworkConnectivity
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.wallet.R
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
                val cardId = globalState?.scanResponse?.card?.cardId
                cardId?.let { preferencesStorage.usedCardsPrefStorage.scanned(it) }
            }
            is WalletAction.Warnings.AppRating.RemindLater -> {
                preferencesStorage.appRatingLaunchObserver.applyDelayedShowing()
            }
            is WalletAction.Warnings.AppRating.SetNeverToShow -> {
                preferencesStorage.appRatingLaunchObserver.setNeverToShow()
            }
            is WalletAction.Warnings.CheckRemainingSignatures -> {
                if (action.remainingSignatures != null &&
                        action.remainingSignatures <= WarningMessagesManager.REMAINING_SIGNATURES_WARNING
                ) {
                    store.state.globalState.warningManager
                            ?.removeWarnings(
                                messageRes = R.string.warning_low_signatures_format
                            )
                    addWarningMessage(
                        warning =
                        WarningMessagesManager.remainingSignaturesNotEnough(action.remainingSignatures),
                        autoUpdate = true
                    )
                }
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
            store.state.globalState.analyticsHandlers?.triggerEvent(AnalyticsEvent.APP_RATING_DISPLAYED)
            addWarningMessage(WarningMessagesManager.appRatingWarning(), true)
        }
    }

    private fun showCardWarningsIfNeeded(globalState: GlobalState?) {
        globalState?.scanResponse?.let { scanResponse ->
            val card = scanResponse.card
            globalState.warningManager?.removeWarnings(WarningMessage.Origin.Local)
            if (card.isTestCard) {
                addWarningMessage(WarningMessagesManager.testCardWarning(), autoUpdate = true)
                return@let
            }

            showWarningLowRemainingSignaturesIfNeeded(card)
            if (card.firmwareVersion.type != FirmwareVersion.FirmwareType.Release) {
                addWarningMessage(WarningMessagesManager.devCardWarning())
            } else if (!preferencesStorage.usedCardsPrefStorage.wasScanned(card.cardId)) {
                checkIfWarningNeeded(scanResponse)?.let { warning -> addWarningMessage(warning) }
            }
            if (card.firmwareVersion.type == FirmwareVersion.FirmwareType.Release) {
                if (!globalState.cardVerifiedOnline) {
                    addWarningMessage(WarningMessagesManager.onlineVerificationFailed())
                }
            }
            setWarningMessages()
        }
    }

    private fun showWarningLowRemainingSignaturesIfNeeded(card: Card) {
        val remainingSignatures = card.remainingSignatures
        if (remainingSignatures != null &&
                remainingSignatures <= WarningMessagesManager.REMAINING_SIGNATURES_WARNING
        ) {
            addWarningMessage(
                WarningMessagesManager.remainingSignaturesNotEnough(
                    remainingSignatures
                )
            )
        }
    }

    private fun checkIfWarningNeeded(
        scanResponse: ScanResponse,
    ): WarningMessage? {
        if (scanResponse.isTangemTwins()) return null

        if (scanResponse.card.isMultiwalletAllowed) {
            return if (scanResponse.card.hasSignedHashes()) {
                WarningMessagesManager.signedHashesMultiWalletWarning()
            } else {
                store.dispatch(WalletAction.Warnings.CheckHashesCount.SaveCardId)
                null
            }
        }

        val validator = store.state.walletState.walletManagers.firstOrNull()
                as? SignatureCountValidator
        return if (validator == null) {
            if (scanResponse.card.hasSignedHashes()) {
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

        val scanResponse = store.state.globalState.scanResponse
        val card = scanResponse?.card
        if (card == null || preferencesStorage.usedCardsPrefStorage.wasScanned(card.cardId)) return

        if (scanResponse.isTangemTwins() || card.isMultiwalletAllowed) return

        val validator = store.state.walletState.walletManagers.firstOrNull()
                as? SignatureCountValidator
        scope.launch {
            val signedHashes = card.getSingleWallet()?.totalSignedHashes ?: 0
            val result = validator?.validateSignatureCount(signedHashes)
            withContext(Dispatchers.Main) {
                when (result) {
                    SimpleResult.Success -> {
                        store.dispatch(WalletAction.Warnings.CheckHashesCount.ConfirmHashesCount)
                        store.dispatch(WalletAction.Warnings.CheckHashesCount.SaveCardId)
                    }
                    is SimpleResult.Failure ->
                        if (result.error is BlockchainSdkError.SignatureCountNotMatched) {
                            addWarningMessage(
                                WarningMessagesManager.alreadySignedHashesWarning(),
                                true
                            )
                        } else if (signedHashes > 0) {
                            addWarningMessage(
                                WarningMessagesManager.alreadySignedHashesWarning(),
                                true
                            )
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
        return warningManager.getWarnings(
            WarningMessage.Location.MainScreen,
            store.state.walletState.blockchains
        )
    }
}