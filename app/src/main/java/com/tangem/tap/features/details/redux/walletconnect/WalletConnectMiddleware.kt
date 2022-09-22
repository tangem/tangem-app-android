package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.card.Card
import com.tangem.common.extensions.guard
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppDialog
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
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

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
                val uri = action.copiedUri
                if (uri != null && WalletConnectManager.isCorrectWcUri(uri)) {
                    store.dispatchOnMain(WalletConnectAction.ShowClipboardOrScanQrDialog(uri))
                } else {
                    store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.QrScan))
                }
            }
            is WalletConnectAction.SelectNetwork -> {
                store.dispatch(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.ChooseNetwork(
                            session = action.session,
                            networks = action.networks,
                        ),
                    ),
                )
            }
            is WalletConnectAction.ChooseNetwork -> {
                val data = state()?.walletConnectState?.newSessionData ?: return
                scope.launch {
                    prepareWalletManager(data.scanResponse, store.state.walletState, action.blockchain, data.session)
                }
            }
            is WalletConnectAction.ShowClipboardOrScanQrDialog -> {
                store.dispatchOnMain(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.ClipboardOrScanQr(
                            action.wcUri,
                        ),
                    ),
                )
            }
            is WalletConnectAction.OpeningSessionTimeout -> {
                store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.SessionTimeout))
            }
            is WalletConnectAction.FailureEstablishingSession -> {
                if (action.error != null) {
                    store.dispatch(
                        GlobalAction.ShowDialog(
                            AppDialog.SimpleOkDialogRes(
                                headerId = R.string.common_warning,
                                messageId = action.error.messageResource,
                            ),
                        ),
                    )
                }
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
                        WalletConnectDialog.OpeningSessionRejected,
                    ),
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
                    type = action.type,
                )
            }
            is WalletConnectAction.HandlePersonalSignRequest -> {
                walletConnectManager.handlePersonalSignRequest(
                    message = action.message,
                    session = action.session,
                    id = action.id,
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
                            dAppName = action.sessionData.peerMeta.name,
                        ),
                    ),
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
                            dAppName = action.sessionData.peerMeta.name,
                        ),
                    ),
                )
            }
            is WalletConnectAction.BinanceTransaction.Sign -> {
                walletConnectManager.signBnb(
                    action.id, action.data, action.sessionData,
                )
            }
            is WalletConnectAction.SwitchBlockchain -> {
                if (action.session.wallet.derivationStyle == DerivationStyle.LEGACY) {
                    Timber.d("Cannot switch chains on AC01/AC02 wallets")
                    return
                }
                val blockchain = action.blockchain.guard {
                    store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.UnsupportedNetwork))
                    return
                }
                val factory = WcWalletManagerFactory(
                    factory = store.state.globalState.tapWalletManager.walletManagerFactory,
                    currenciesRepository = currenciesRepository,
                )
                val walletState = store.state.walletState
                scope.launch {
                    val walletManager = factory.getWalletManager(
                        wallet = action.session.wallet,
                        blockchain = blockchain,
                        walletState = walletState,
                    ).guard {
                        store.dispatchOnMain(
                            GlobalAction.ShowDialog(
                                WalletConnectDialog.AddNetwork(blockchain.fullName),
                            ),
                        )
                        return@launch
                    }
                    val updatedWallet = action.session.wallet.copy(
                        walletPublicKey = walletManager.wallet.publicKey.seedKey,
                        derivedPublicKey = walletManager.wallet.publicKey.derivedKey,
                        derivationPath = walletManager.wallet.publicKey.derivationPath,
                        blockchain = action.blockchain,
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
            chainId = chainId,
            peer = session.peerMeta,
        ) ?: Blockchain.Ethereum

        store.dispatch(
            GlobalAction.ScanCard(
                additionalBlockchainsToDerive = listOf(blockchain),
                onSuccess = { scanResponse ->
                    handleScanResponse(scanResponse = scanResponse, session = session, blockchain = blockchain)
                },
                onFailure = {
                    store.dispatchOnMain(WalletConnectAction.FailureEstablishingSession(null))
                },
                R.string.wallet_connect_scan_card_message,
            ),
        )
    }

    private suspend fun getAvailableBlockchains(card: Card, walletState: WalletState): List<Blockchain> {
        return if (walletState.cardId == card.cardId) {
            walletState.currencies.filter {
                it.isBlockchain() && !it.isCustomCurrency(card.derivationStyle) && it.blockchain.isEvm()
            }.map { it.blockchain }
        } else {
            currenciesRepository
                .loadSavedCurrencies(card.cardId, card.settings.isHDWalletAllowed)
                .filter {
                    it.blockchain.isEvm() &&
                        it.derivationPath == it.blockchain.derivationPath(card.derivationStyle)?.rawPath
                }
                .map { it.blockchain }
        }
    }

    private suspend fun prepareWalletManager(
        scanResponse: ScanResponse,
        walletState: WalletState,
        blockchain: Blockchain,
        session: WalletConnectSession,
    ) {
        val factory = WcWalletManagerFactory(
            factory = store.state.globalState.tapWalletManager.walletManagerFactory,
            currenciesRepository = currenciesRepository,
        )
        val walletManager = factory.getWalletManager(scanResponse, blockchain, walletState).guard {
            store.dispatchOnMain(WalletConnectAction.FailureEstablishingSession(session.session))
            store.dispatchOnMain(
                GlobalAction.ShowDialog(
                    WalletConnectDialog.AddNetwork(blockchain.fullName),
                ),
            )
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
            derivationStyle = scanResponse.card.derivationStyle,
            blockchain = wallet.blockchain,
        )

        withMainContext {
            val updatedSession = session.copy(wallet = walletForSession)
            walletConnectManager.updateSession(updatedSession)

            store.dispatch(WalletConnectAction.ApproveSession(session.session))
        }
    }

    private fun handleScanResponse(
        scanResponse: ScanResponse,
        session: WalletConnectSession,
        blockchain: Blockchain,
    ) {
        val card = scanResponse.card
        if (!card.isMultiwalletAllowed) {
            store.dispatchOnMain(WalletConnectAction.UnsupportedCard)
            return
        }
        val walletState = store.state.walletState
        val updatedSession = session.copy(wallet = session.wallet.copy(blockchain = blockchain))
        store.dispatch(
            WalletConnectAction.SetNewSessionData(
                NewWcSessionData(session = updatedSession, scanResponse = scanResponse, blockchain = blockchain),
            ),
        )

        scope.launch {
            val blockchains = if (blockchain.isEvm()) getAvailableBlockchains(card, walletState) else emptyList()

            withMainContext {
                store.dispatch(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.ApproveWcSession(session = updatedSession, networks = blockchains),
                    ),
                )
            }
        }
    }
}