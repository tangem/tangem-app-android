package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.guard
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.extensions.isMultiwalletAllowed
import com.tangem.tap.domain.walletconnect.BnbHelper
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.domain.walletconnect.WalletConnectNetworkUtils
import com.tangem.tap.domain.walletconnect.WcWalletManagerFactory
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

class WalletConnectMiddleware {
    private val walletConnectManager = WalletConnectManager()

    val walletConnectMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                handle(state, action)
                next(action)
            }
        }
    }

    private fun handle(state: () -> AppState?, action: Action) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is WalletConnectAction.RestoreSessions -> {
                walletConnectManager.restoreSessions()
            }
            is WalletConnectAction.HandleDeepLink -> {
                if (!action.wcUri.isNullOrBlank()) {
                    if (WalletConnectManager.isCorrectWcUri(action.wcUri)) {
                        store.dispatchOnMain(WalletConnectAction.OpenSession(action.wcUri))
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
                store.dispatchOnMain(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.ClipboardOrScanQr(
                            action.wcUri
                        )
                    )
                )
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
                )
            }
            is WalletConnectAction.RefuseOpeningSession -> {
                store.dispatch(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.OpeningSessionRejected
                    )
                )
            }
            is WalletConnectAction.ScanCard -> {
                scanCard(action.session, action.chainId)
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
            is WalletConnectAction.BinanceTransaction.Trade -> {
                val messageData = BnbHelper.createMessageData(action.order)
                store.dispatchOnMain(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.BnbTransactionDialog(
                            data = messageData,
                            session = action.sessionData.session,
                            sessionId = action.id,
                            cardId = action.sessionData.wallet.cardId,
                            dAppName = action.sessionData.peerMeta.name
                        )
                    )
                )
            }
            is WalletConnectAction.BinanceTransaction.Transfer -> {
                val messageData = BnbHelper.createMessageData(action.order)
                store.dispatchOnMain(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.BnbTransactionDialog(
                            data = messageData,
                            session = action.sessionData.session,
                            sessionId = action.id,
                            cardId = action.sessionData.wallet.cardId,
                            dAppName = action.sessionData.peerMeta.name
                        )
                    )
                )
            }
            is WalletConnectAction.BinanceTransaction.Sign -> {
                walletConnectManager.signBnb(
                    action.id, action.data, action.sessionData
                )
            }
            is WalletConnectAction.SwitchBlockchain -> {
                val blockchain = action.blockchain.guard {
                    store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.UnsupportedCard)) // TODO: add relevant dialog
                    return
                }

                val factory = WcWalletManagerFactory(
                    factory = store.state.globalState.tapWalletManager.walletManagerFactory,
                    currenciesRepository = currenciesRepository
                )
                val walletState = store.state.walletState
                scope.launch {
                    val walletManager = factory.getWalletManager(
                            wallet = action.session.wallet,
                            blockchain = blockchain,
                            walletState = walletState
                        ).guard {
                        store.dispatchOnMain(
                            GlobalAction.ShowDialog(
                                WalletConnectDialog.AddNetwork(blockchain.fullName)
                            )
                        )
                        return@launch
                    }

                    val updatedWallet = action.session.wallet.copy(
                        walletPublicKey = walletManager.wallet.publicKey.seedKey,
                        derivedPublicKey = walletManager.wallet.publicKey.derivedKey,
                        derivationPath = walletManager.wallet.publicKey.derivationPath,
                        blockchain = action.blockchain

                    )
                    val updatedSession = action.session.copy(wallet = updatedWallet)
                    store.dispatchOnMain(WalletConnectAction.UpdateBlockchain(updatedSession))
                }
            }
            is WalletConnectAction.UpdateBlockchain -> {
                walletConnectManager.updateBlockchain(action.updatedSession)
            }
        }
    }

    private fun scanCard(session: WalletConnectSession, chainId: Int?) {
        val blockchain = WalletConnectNetworkUtils.parseBlockchain(
            chainId = chainId, peer = session.peerMeta
        ) ?: Blockchain.Ethereum

        store.dispatch(
            GlobalAction.ScanCard(
                additionalBlockchainsToDerive = listOf(blockchain),
                onSuccess = { scanResponse ->
                    handleScanResponse(scanResponse, session, blockchain)
                },
                onFailure = {
                    store.dispatchOnMain(WalletConnectAction.FailureEstablishingSession(null))
                }, R.string.wallet_connect_scan_card_message
            )
        )
    }

    private fun handleScanResponse(
        scanResponse: ScanResponse, session: WalletConnectSession, blockchain: Blockchain
    ) {
        val card = scanResponse.card
        if (!card.isMultiwalletAllowed) {
            store.dispatchOnMain(WalletConnectAction.UnsupportedCard)
            return
        }

        val factory = WcWalletManagerFactory(
            factory = store.state.globalState.tapWalletManager.walletManagerFactory,
            currenciesRepository = currenciesRepository
        )
        val walletState = store.state.walletState
        scope.launch {
            val walletManager = factory.getWalletManager(scanResponse, blockchain, walletState).guard {
                store.dispatchOnMain(WalletConnectAction.FailureEstablishingSession(session.session))
                store.dispatchOnMain(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.AddNetwork(blockchain.fullName)
                    )
                )
                return@launch
            }

            val wallet = walletManager.wallet
            val derivedKey =
                if (wallet.publicKey.blockchainKey.contentEquals(wallet.publicKey.seedKey)) {
                    null
                } else {
                    walletManager.wallet.publicKey.blockchainKey
                }
            val walletForSession = WalletForSession(
                cardId = scanResponse.card.cardId,
                walletPublicKey = wallet.publicKey.seedKey,
                derivedPublicKey = derivedKey,
                derivationPath = wallet.publicKey.derivationPath,
                blockchain = wallet.blockchain
            )

            withMainContext {
                val updatedSession = session.copy(wallet = walletForSession)
                walletConnectManager.updateSession(updatedSession)

                store.dispatch(WalletConnectAction.AddScanResponse(scanResponse))

                store.dispatch(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.ApproveWcSession(
                            updatedSession
                        )
                    )
                )
            }
        }

    }
}