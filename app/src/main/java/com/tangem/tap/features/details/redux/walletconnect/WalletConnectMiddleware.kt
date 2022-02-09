package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.guard
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.isMultiwalletAllowed
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.walletconnect.BnbHelper
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.domain.walletconnect.WalletConnectNetworkUtils
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
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
                }
                next(action)
            }
        }
    }

    private fun scanCard(session: WalletConnectSession, chainId: Int?) {
        val blockchain = WalletConnectNetworkUtils.parseBlockchain(
            chainId = chainId, peer = session.peerMeta
        ) ?: Blockchain.Ethereum

        store.dispatch(GlobalAction.ScanCard(additionalBlockchainsToDerive = listOf(blockchain),
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

        val walletManager = getWalletManager(scanResponse, blockchain).guard {
                store.dispatchOnMain(WalletConnectAction.FailureEstablishingSession(null))
                return
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
        val updatedSession = session.copy(wallet = walletForSession)
        walletConnectManager.updateSession(updatedSession)

        store.dispatchOnMain(WalletConnectAction.AddScanResponse(scanResponse))

        store.dispatchOnMain(
            GlobalAction.ShowDialog(
                WalletConnectDialog.ApproveWcSession(
                    updatedSession
                )
            )
        )
    }

    private fun getWalletManager(
        scanResponse: ScanResponse, blockchain: Blockchain
    ): WalletManager? {
        val card = scanResponse.card
        val factory = store.state.globalState.tapWalletManager.walletManagerFactory
        val blockchainToMake = if (blockchain == Blockchain.Ethereum && card.isTestCard)  {
            Blockchain.EthereumTestnet
        } else {
            blockchain
        }

        return if (store.state.globalState.scanResponse?.card?.cardId == card.cardId) {
            store.state.walletState.getWalletManager(blockchainToMake)
                ?: factory.makeWalletManagerForApp(scanResponse, blockchainToMake)
                    ?.also { walletManager ->
                        store.dispatch(WalletAction.MultiWallet.AddWalletManagers(walletManager))
                        store.dispatch(WalletAction.MultiWallet.AddBlockchain(walletManager.wallet.blockchain))
                    }
        } else {
            val walletManager = factory.makeWalletManagerForApp(scanResponse, blockchainToMake)
            if (currenciesRepository.loadCardCurrencies(card.cardId)?.blockchains?.contains(
                    blockchainToMake
                ) != true
            ) {
                walletManager?.let {
                    currenciesRepository.saveAddedBlockchain(
                        card.cardId,
                        blockchainToMake
                    )
                }
            }
            return walletManager
        }
    }
}