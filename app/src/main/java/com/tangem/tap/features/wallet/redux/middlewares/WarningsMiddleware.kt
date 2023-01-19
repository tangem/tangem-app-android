package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.SignatureCountValidator
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.extensions.hasSignedHashes
import com.tangem.tap.features.demo.isDemoCard
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
            is WalletAction.Warnings.Update -> setWarningMessages()
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
                    store.state.globalState.warningManager?.removeWarnings(R.string.warning_low_signatures_format)
                    addWarningMessage(
                        warning = WarningMessagesManager.remainingSignaturesNotEnough(action.remainingSignatures),
                        autoUpdate = true,
                    )
                }
            }
            is WalletAction.Warnings.AppRating,
            is WalletAction.Warnings.CheckHashesCount,
            is WalletAction.Warnings.CheckHashesCount.ConfirmHashesCount,
            is WalletAction.Warnings.CheckHashesCount.NeedToCheckHashesCountOnline,
            is WalletAction.Warnings.Set,
            -> Unit
        }
    }

    fun tryToShowAppRatingWarning(hasNonZeroWallets: Boolean) {
        if (hasNonZeroWallets) {
            preferencesStorage.appRatingLaunchObserver.foundWalletWithFunds()
        }
        if (preferencesStorage.appRatingLaunchObserver.isReadyToShow()) {
            addWarningMessage(WarningMessagesManager.appRatingWarning(), true)
        }
    }

    fun tryToShowAppRatingWarning(wallet: Wallet) {
        val nonZeroWalletsCount = wallet.amounts.filter {
            it.value.value?.isGreaterThan(BigDecimal.ZERO) ?: false
        }.size
        tryToShowAppRatingWarning(hasNonZeroWallets = nonZeroWalletsCount > 0)
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
            if (card.firmwareVersion.type == FirmwareVersion.FirmwareType.Release && !globalState.cardVerifiedOnline) {
                addWarningMessage(WarningMessagesManager.onlineVerificationFailed())
            }
            if (scanResponse.isDemoCard()) {
                addWarningMessage(WarningMessagesManager.demoCardWarning())
            }
            setWarningMessages()
        }
    }

    private fun showWarningLowRemainingSignaturesIfNeeded(card: CardDTO) {
        val remainingSignatures = card.wallets.firstOrNull()?.remainingSignatures
        if (remainingSignatures != null &&
            remainingSignatures <= WarningMessagesManager.REMAINING_SIGNATURES_WARNING
        ) {
            addWarningMessage(WarningMessagesManager.remainingSignaturesNotEnough(remainingSignatures))
        }
    }

    private fun checkIfWarningNeeded(
        scanResponse: ScanResponse,
    ): WarningMessage? {
        if (scanResponse.cardTypesResolver.isTangemTwins() || scanResponse.isDemoCard()) return null

        if (scanResponse.cardTypesResolver.isMultiwalletAllowed()) {
            val isBackupForbidden = with(scanResponse.card.settings) { !(isBackupAllowed || isHDWalletAllowed) }
            return if (scanResponse.card.hasSignedHashes() && isBackupForbidden) {
                WarningMessagesManager.signedHashesMultiWalletWarning()
            } else {
                store.dispatch(WalletAction.Warnings.CheckHashesCount.SaveCardId)
                null
            }
        }

        val validator = store.state.walletState.walletManagers.firstOrNull() as? SignatureCountValidator
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

        if (scanResponse.cardTypesResolver.isTangemTwins() || scanResponse.cardTypesResolver.isMultiwalletAllowed()) {
            return
        }

        val validator = store.state.walletState.walletManagers.firstOrNull()
            as? SignatureCountValidator
        scope.launch {
            val signedHashes = card.wallets.firstOrNull()?.totalSignedHashes ?: 0
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
                    null -> Unit
                }
            }
        }
    }

    private fun addWarningMessage(warning: WarningMessage, autoUpdate: Boolean = false) {
        store.state.globalState.warningManager?.addWarning(warning)
        if (autoUpdate) setWarningMessages()
    }

    private fun setWarningMessages() {
        store.dispatchOnMain(WalletAction.Warnings.Set(getWarnings()))
    }

    private fun getWarnings(): List<WarningMessage> {
        val warningManager = store.state.globalState.warningManager ?: return emptyList()
        return warningManager.getWarnings(
            WarningMessage.Location.MainScreen,
            store.state.walletState.blockchains,
        )
    }
}
