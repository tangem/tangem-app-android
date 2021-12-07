package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.blockchain.common.*
import com.tangem.common.extensions.guard
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.isMultiwalletAllowed
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.wallet.R
import org.rekotlin.Middleware

class WalletConnectMiddleware {
    private val walletConnectManager = WalletConnectManager()

    val walletConnectMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {

                    is WalletConnectAction.RestoreSessions -> {
                        walletConnectManager.restoreSessions()
                    }

                    is WalletConnectAction.HandleDeepLink -> {
                        if (!action.wcUri.isNullOrBlank()) {
                            if (WalletConnectManager.isCorrectWcUri(action.wcUri)) {
                                store.dispatchOnMain(WalletConnectAction.ScanCard(action.wcUri))
                            }
                        }
                    }

                    is WalletConnectAction.StartWalletConnect -> {
                        val uri = action.activity.getFromClipboard()?.toString()
                        if (uri != null && WalletConnectManager.isCorrectWcUri(uri)) {
                            store.dispatchOnMain(WalletConnectAction.ShowClipboardOrScanQrDialog(uri))
                        } else {
                            store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.QrScan))
                        }
                    }

                    is WalletConnectAction.ShowClipboardOrScanQrDialog -> {
                        store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.ClipboardOrScanQr(
                            action.wcUri)))
                    }

                    is WalletConnectAction.ScanCard -> {
                        handleScanCard(action.wcUri)
                    }

                    is WalletConnectAction.OpeningSessionTimeout -> {
                        store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.SessionTimeout))
                    }

                    is WalletConnectAction.FailureEstablishingSession -> {
                        if (action.session != null) {
                            walletConnectManager.disconnect(action.session)
                        }
                    }

                    is WalletConnectAction.UnsupportedCard -> {
                        store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.UnsupportedCard))
                    }

                    is WalletConnectAction.OpenSession -> {
                        walletConnectManager.connect(
                            wcUri = action.wcUri,
                            wallet = action.wallet
                        )
                    }

                    is WalletConnectAction.RefuseOpeningSession -> {
                        store.dispatch(GlobalAction.ShowDialog(
                            WalletConnectDialog.OpeningSessionRejected
                        ))
                    }

                    is WalletConnectAction.AcceptOpeningSession -> {
                        store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.ApproveWcSession(
                            action.session)))
                    }

                    is WalletConnectAction.ApproveSession -> {
                        walletConnectManager.approve(action.session)
                    }

                    is WalletConnectAction.DisconnectSession -> {
                        walletConnectManager.disconnect(action.session)
                    }

                    is WalletConnectAction.HandleTransactionRequest -> {
                        walletConnectManager.handleTransactionRequest(
                            transaction = action.transaction,
                            session = action.session,
                            id = action.id,
                            type = action.type
                        )
                    }
                    is WalletConnectAction.HandlePersonalSignRequest -> {
                        walletConnectManager.handlePersonalSignRequest(
                            message = action.message,
                            session = action.session,
                            id = action.id
                        )
                    }
                    is WalletConnectAction.RejectRequest -> {
                        walletConnectManager.rejectRequest(action.session, action.id)
                    }
                    is WalletConnectAction.SendTransaction -> {
                        walletConnectManager.completeTransaction(action.session)
                    }
                    is WalletConnectAction.SignMessage -> {
                        walletConnectManager.sendSignedMessage(action.session)
                    }
                }
                next(action)
            }
        }
    }

    private fun handleScanCard(wcUri: String) {
        store.dispatch(GlobalAction.ScanCard(true, { scanResponse ->
            val card = scanResponse.card
            if (!card.isMultiwalletAllowed) {
                store.dispatchOnMain(WalletConnectAction.UnsupportedCard)
                return@ScanCard
            }

            val walletManager = getWalletManager(scanResponse).guard {
                store.dispatchOnMain(WalletConnectAction.UnsupportedCard)
                return@ScanCard
            }

            val wallet = walletManager.wallet
            val derivedKey = if (wallet.publicKey.blockchainKey.contentEquals(wallet.publicKey.seedKey)) {
                null
            } else {
                walletManager.wallet.publicKey.blockchainKey
            }
            store.dispatchOnMain(WalletConnectAction.OpenSession(
                wcUri = wcUri,
                wallet = WalletForSession(
                    card.cardId,
                    wallet.publicKey.seedKey,
                    derivedKey,
                    wallet.publicKey.derivationPath,
                    card.isTestCard
                ),
            ))
        }, {
            store.dispatchOnMain(WalletConnectAction.FailureEstablishingSession(null))
        }, R.string.wallet_connect_scan_card_message))
    }

    private fun getWalletManager(scanResponse: ScanResponse): WalletManager? {
        val card = scanResponse.card
        val factory = store.state.globalState.tapWalletManager.walletManagerFactory

        return if (store.state.globalState.scanResponse?.card?.cardId == card.cardId) {
            store.state.walletState.getWalletManager(Blockchain.Ethereum)
                ?: factory.makeWalletManagerForApp(scanResponse, Blockchain.Ethereum)
                    ?.also { walletManager ->
                        store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManager))
                        store.dispatch(WalletAction.MultiWallet.AddBlockchain(walletManager.wallet.blockchain))
                    }
        } else {
            if (currenciesRepository.loadCardCurrencies(card.cardId)?.blockchains?.contains(
                    Blockchain.Ethereum) == true
            ) {
                factory.makeWalletManagerForApp(scanResponse, Blockchain.Ethereum)
            } else {
                factory.makeWalletManagerForApp(scanResponse, Blockchain.Ethereum)
                    ?.also {
                        currenciesRepository.saveAddedBlockchain(card.cardId, Blockchain.Ethereum)
                    }
            }
        }
    }
}