package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.extensions.guard
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.derivationPath
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect.BnbHelper
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.domain.walletconnect.WalletConnectNetworkUtils
import com.tangem.tap.domain.walletconnect.extensions.toWcEthTransaction
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.domain.walletconnect2.domain.models.Account
import com.tangem.tap.domain.walletconnect2.domain.models.BnbData
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

@Suppress("LargeClass")
class WalletConnectMiddleware {
    private var walletConnectManager = WalletConnectManager()
    private val walletConnectInteractor: WalletConnectInteractor
        get() = store.state.daggerGraphState.get(DaggerGraphState::walletConnectInteractor)
    private val walletConnectRepository: WalletConnectRepository
        get() = store.state.daggerGraphState.get(DaggerGraphState::walletConnectRepository)

    val walletConnectMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                handle(state, action)
                next(action)
            }
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun handle(state: () -> AppState?, action: Action) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is WalletConnectAction.ResetState -> walletConnectManager = WalletConnectManager()
            is WalletConnectAction.RestoreSessions -> {
                walletConnectManager.restoreSessions(action.scanResponse)
            }
            is WalletConnectAction.HandleDeepLink -> {
                if (!action.wcUri.isNullOrBlank()) {
                    store.dispatchOnMain(WalletConnectAction.OpenSession(action.wcUri))
                }
            }
            is WalletConnectAction.StartWalletConnect -> {
                val uri = action.copiedUri
                if (uri != null && isWalletConnectUri(uri)) {
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
                    prepareWalletManager(
                        scanResponse = data.scanResponse,
                        walletState = store.state.walletState,
                        blockchain = action.blockchain,
                        session = data.session,
                        walletConnectManager = walletConnectManager,
                    )
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
                Timber.e("OpeningSessionTimeout for topic ${action.session.topic}")
                // do not show dialog for now, it shows always to user if cannot establish connection
                // store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.SessionTimeout))
            }
            is WalletConnectAction.FailureEstablishingSession -> {
                Timber.e("FailureEstablishingSession for topic ${action.session?.topic}")
                // disable alerts in release to avoid annoying users
                // if (action.error != null) {
                //     store.dispatch(
                //         GlobalAction.ShowDialog(
                //             AppDialog.SimpleOkDialogRes(
                //                 headerId = R.string.common_warning,
                //                 messageId = action.error.messageResource,
                //             ),
                //         ),
                //     )
                // }
                if (action.session != null) {
                    walletConnectManager.disconnect(action.session)
                }
            }
            is WalletConnectAction.UnsupportedCard -> {
                store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.UnsupportedCard))
            }
            is WalletConnectAction.OpenSession -> {
                val index = action.wcUri.indexOf("@")
                when (action.wcUri[index + 1]) {
                    '1' -> {
                        walletConnectManager.connect(wcUri = action.wcUri)
                    }
                    '2' -> walletConnectRepository.pair(uri = action.wcUri)
                }
            }
            is WalletConnectAction.RefuseOpeningSession -> {
                Timber.e("RefuseOpeningSession")

                // do not show for now to avoid anoying users with alert
                // store.dispatch(
                //     GlobalAction.ShowDialog(
                //         WalletConnectDialog.OpeningSessionRejected,
                //     ),
                // )
            }
            is WalletConnectAction.ScanCard -> {
                val scanResponse = store.state.globalState.scanResponse ?: return
                scanCard(scanResponse, action.session, action.chainId)
            }
            is WalletConnectAction.ApproveSession -> {
                walletConnectManager.approve(action.session)
            }
            is WalletConnectAction.DisconnectSession -> {
                if (action.session != null) {
                    walletConnectManager.disconnect(action.session)
                } else {
                    walletConnectInteractor.disconnectSession(action.topic)
                }
            }
            is WalletConnectAction.HandleTransactionRequest -> {
                walletConnectManager.handleTransactionRequest(
                    transaction = action.transaction.toWcEthTransaction(),
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
                walletConnectManager.rejectRequest(action.topic, action.id)
                walletConnectInteractor.cancelRequest(action.topic, action.id)
            }
            is WalletConnectAction.SendTransaction -> {
                walletConnectManager.completeTransaction(action.topic)
            }
            is WalletConnectAction.SignMessage -> {
                walletConnectManager.sendSignedMessage(action.topic)
            }
            is WalletConnectAction.BinanceTransaction.Trade -> {
                val messageData = BnbHelper.createMessageData(action.order)
                store.dispatchOnMain(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.BnbTransactionDialog(
                            WcPreparedRequest.BnbTransaction(
                                preparedRequestData = BnbData(
                                    data = messageData,
                                    topic = action.sessionData.session.topic,
                                    requestId = action.id,
                                    dAppName = action.sessionData.peerMeta.name,
                                ),
                                topic = action.sessionData.session.topic,
                                requestId = action.id,
                                derivationPath = action.sessionData.wallet.derivationPath?.rawPath,
                            ),
                        ),
                    ),
                )
            }
            is WalletConnectAction.BinanceTransaction.Transfer -> {
                val messageData = BnbHelper.createMessageData(action.order)
                store.dispatchOnMain(
                    GlobalAction.ShowDialog(
                        WalletConnectDialog.BnbTransactionDialog(
                            WcPreparedRequest.BnbTransaction(
                                preparedRequestData = BnbData(
                                    data = messageData,
                                    topic = action.sessionData.session.topic,
                                    requestId = action.id,
                                    dAppName = action.sessionData.peerMeta.name,
                                ),
                                topic = action.sessionData.session.topic,
                                requestId = action.id,
                                derivationPath = action.sessionData.wallet.derivationPath?.rawPath,
                            ),
                        ),
                    ),
                )
            }
            is WalletConnectAction.BinanceTransaction.Sign -> {
                walletConnectManager.signBnb(
                    id = action.id,
                    data = action.data,
                    topic = action.topic,
                )
            }
            is WalletConnectAction.SwitchBlockchain -> {
                if (action.session.wallet.derivationStyle == DerivationStyle.LEGACY) {
                    Timber.d("Cannot switch chains on AC01/AC02 wallets")
                    return
                }
                val blockchain = action.blockchain.guard {
                    store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.UnsupportedNetwork()))
                    return
                }
                val walletManager = getWalletManager(
                    wallet = action.session.wallet,
                    blockchain = blockchain,
                    walletState = store.state.walletState,
                ).guard {
                    store.dispatchOnMain(
                        GlobalAction.ShowDialog(
                            WalletConnectDialog.AddNetwork(blockchain.fullName),
                        ),
                    )
                    return
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
            is WalletConnectAction.UpdateBlockchain -> {
                walletConnectManager.updateBlockchain(action.updatedSession)
            }
            is WalletConnectAction.ApproveProposal -> {
                val accounts = store.state.walletState.walletManagers
                    .mapNotNull {
                        val wallet = it.wallet
                        val chainId = walletConnectInteractor.blockchainHelper.networkIdToChainIdOrNull(
                            wallet.blockchain.toNetworkId(),
                        )
                        chainId?.let {
                            Account(
                                chainId,
                                wallet.address,
                                wallet.publicKey.derivationPath?.rawPath,
                            )
                        }
                    }

                walletConnectInteractor.approveSessionProposal(accounts)
            }
            is WalletConnectAction.RejectProposal -> {
                walletConnectInteractor.rejectSessionProposal()
            }
            is WalletConnectAction.SessionEstablished -> {
            }
            is WalletConnectAction.SessionRejected -> {
                when (action.error) {
                    is WalletConnectError.ApprovalErrorAddNetwork -> {
                        val blockchains = action.error.networks.mapNotNull { Blockchain.fromNetworkId(it)?.fullName }
                        store.dispatchOnMain(
                            GlobalAction.ShowDialog(
                                WalletConnectDialog.AddNetwork(blockchains.toString()),
                            ),
                        )
                    }
                    is WalletConnectError.ApprovalErrorUnsupportedNetwork -> {
                        store.dispatchOnMain(
                            GlobalAction.ShowDialog(
                                WalletConnectDialog.UnsupportedNetwork(action.error.unsupportedNetworks),
                            ),
                        )
                    }
                    is WalletConnectError.ExternalApprovalError -> {
                        Timber.e(action.error, "ExternalApprovalError ${action.error.message}")
                        // do not show dialog on this event
                        // val message = action.error.message
                        // if (!message.isNullOrEmpty()) {
                        //     store.dispatchOnMain(
                        //         GlobalAction.ShowDialog(
                        //             AppDialog.SimpleOkWarningDialog(
                        //                 message = message,
                        //             ),
                        //         ),
                        //     )
                        // }
                    }
                    else -> Unit
                }
            }

            is WalletConnectAction.ShowSessionRequest -> {
                val dialog: WalletConnectDialog = when (val request = action.sessionRequest) {
                    is WcPreparedRequest.BnbTransaction -> WalletConnectDialog.BnbTransactionDialog(request)
                    is WcPreparedRequest.EthTransaction -> WalletConnectDialog.RequestTransaction(request)
                    is WcPreparedRequest.EthSign -> WalletConnectDialog.PersonalSign(request)
                }
                store.dispatch(GlobalAction.ShowDialog(dialog))
            }
            is WalletConnectAction.PerformRequestedAction -> {
                scope.launch { walletConnectInteractor.continueWithRequest(action.sessionRequest) }
            }
            is WalletConnectAction.RejectUnsupportedRequest -> {
                store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.UnsupportedNetwork()))
            }
        }
    }

    private fun scanCard(scanResponse: ScanResponse, session: WalletConnectSession, chainId: Int?) {
        val blockchain = WalletConnectNetworkUtils.parseBlockchain(
            chainId = chainId,
            peer = session.peerMeta,
        ).guard {
            store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.UnsupportedNetwork()))
            return
        }

        handleScanResponse(scanResponse = scanResponse, session = session, blockchain = blockchain)
    }

    private fun getAvailableBlockchains(
        derivationStyleProvider: DerivationStyleProvider,
        walletState: WalletState,
    ): List<Blockchain> {
        return walletState.currencies.filter {
            it.isBlockchain() &&
                !it.isCustomCurrency(derivationStyleProvider.getDerivationStyle()) && it.blockchain.isEvm()
        }.map { it.blockchain }
    }

    private suspend fun prepareWalletManager(
        scanResponse: ScanResponse,
        walletState: WalletState,
        blockchain: Blockchain,
        session: WalletConnectSession,
        walletConnectManager: WalletConnectManager,
    ) {
        val walletManager = getWalletManager(session.wallet, blockchain, walletState).guard {
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
            walletPublicKey = wallet.publicKey.seedKey,
            derivedPublicKey = derivedKey,
            derivationPath = wallet.publicKey.derivationPath,
            derivationStyle = scanResponse.derivationStyleProvider.getDerivationStyle(),
            blockchain = wallet.blockchain,
        )

        withMainContext {
            val updatedSession = session.copy(wallet = walletForSession)
            walletConnectManager.updateSession(updatedSession)

            store.dispatch(WalletConnectAction.ApproveSession(session.session))
        }
    }

    private fun handleScanResponse(scanResponse: ScanResponse, session: WalletConnectSession, blockchain: Blockchain) {
        if (!scanResponse.cardTypesResolver.isMultiwalletAllowed()) {
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
        val blockchains = if (blockchain.isEvm()) {
            getAvailableBlockchains(scanResponse.derivationStyleProvider, walletState)
        } else {
            emptyList()
        }
        store.dispatch(
            GlobalAction.ShowDialog(
                WalletConnectDialog.ApproveWcSession(session = updatedSession, networks = blockchains),
            ),
        )
    }

    private fun getWalletManager(
        wallet: WalletForSession,
        blockchain: Blockchain,
        walletState: WalletState,
    ): WalletManager? {
        val blockchainToMake = if (blockchain == Blockchain.Ethereum && wallet.isTestNet) {
            Blockchain.EthereumTestnet
        } else {
            blockchain
        }
        val derivation = blockchainToMake.derivationPath(
            style = store.state.globalState.scanResponse?.derivationStyleProvider?.getDerivationStyle(),
        )?.rawPath
        val blockchainNetwork = BlockchainNetwork(
            blockchain = blockchainToMake,
            derivationPath = derivation,
            tokens = emptyList(),
        )
        return walletState.getWalletManager(blockchainNetwork)
    }

    private fun isWalletConnectUri(uri: String): Boolean {
        return WalletConnectManager.isCorrectWcUri(uri) || walletConnectInteractor.isWalletConnectUri(uri)
    }
}
