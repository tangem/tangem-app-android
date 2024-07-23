package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.routing.AppRoute
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect2.domain.LegacyWalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.domain.walletconnect2.domain.models.Account
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

@Suppress("LargeClass")
class WalletConnectMiddleware {
    private val walletConnectInteractor: WalletConnectInteractor
        get() = store.inject(DaggerGraphState::walletConnectInteractor)
    private val walletConnectRepository: LegacyWalletConnectRepository
        get() = store.inject(DaggerGraphState::walletConnectRepository)

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
            is WalletConnectAction.HandleDeepLink -> {
                val wsUrl = action.wcUri
                Timber.i("WC deeplink: $wsUrl")
                if (!wsUrl.isNullOrBlank()) {
                    Timber.i("WC deeplink added to stack: $wsUrl")
                    walletConnectInteractor.addDeeplink(wsUrl)
                }
            }
            is WalletConnectAction.DisconnectSession -> {
                walletConnectInteractor.disconnectSession(action.topic)
            }
            is WalletConnectAction.StartWalletConnect -> {
                val uri = action.copiedUri
                if (uri != null && isWalletConnectUri(uri)) {
                    // TODO check
                    store.dispatchOnMain(WalletConnectAction.ShowClipboardOrScanQrDialog(uri))
                } else {
                    store.dispatchNavigationAction { push(AppRoute.QrScanning(SourceType.WALLET_CONNECT)) }
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
            is WalletConnectAction.OpenSession -> {
                val index = action.wcUri.indexOf("@")
                when (action.wcUri[index + 1]) {
                    '2' -> walletConnectRepository.pair(uri = action.wcUri)
                }
            }
            is WalletConnectAction.RejectRequest -> {
                walletConnectInteractor.cancelRequest(action.topic, action.id)
            }
            is WalletConnectAction.ApproveProposal -> {
                scope.launch {
                    val accounts = getWalletManagers()
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
            }
            is WalletConnectAction.RejectProposal -> {
                walletConnectInteractor.rejectSessionProposal()
            }
            is WalletConnectAction.SessionEstablished -> {
            }
            is WalletConnectAction.SessionRejected -> {
                when (action.error) {
                    is WalletConnectError.ApprovalErrorAddNetwork -> {
                        store.dispatchOnMain(
                            GlobalAction.ShowDialog(
                                WalletConnectDialog.AddNetwork(action.error.networks),
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
                    is WalletConnectError.UnsupportedDApp -> {
                        store.dispatchOnMain(
                            GlobalAction.ShowDialog(
                                WalletConnectDialog.UnsupportedDapp,
                            ),
                        )
                    }
                    is WalletConnectError.UnknownError -> {
                        store.dispatchOnMain(
                            GlobalAction.ShowDialog(
                                AppDialog.SimpleOkDialogRes(
                                    headerId = R.string.wallet_connect_title,
                                    messageId = R.string.wallet_connect_error_with_framework_message,
                                    args = listOf(action.error.message),
                                ),
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
                    is WcPreparedRequest.SignTransaction -> WalletConnectDialog.SignTransactionDialog(request)
                }
                store.dispatch(GlobalAction.ShowDialog(dialog))
            }
            is WalletConnectAction.PerformRequestedAction -> {
                scope.launch { walletConnectInteractor.continueWithRequest(action.sessionRequest) }
            }
            is WalletConnectAction.RejectUnsupportedRequest -> {
                store.dispatchOnMain(GlobalAction.ShowDialog(WalletConnectDialog.UnsupportedNetwork()))
            }
            is WalletConnectAction.PairConnectErrorAction -> {
                store.dispatch(GlobalAction.ShowDialog(WalletConnectDialog.PairConnectErrorDialog(action.throwable)))
            }
        }
    }

    private suspend fun getWalletManagers(): List<WalletManager> {
        val walletManagerFacade = store.inject(DaggerGraphState::walletManagersFacade)
        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
        val userWallet = userWalletsListManager.selectedUserWalletSync ?: return emptyList()

        return walletManagerFacade.getStoredWalletManagers(userWallet.walletId)
    }

    private fun isWalletConnectUri(uri: String): Boolean {
        return walletConnectInteractor.isWalletConnectUri(uri)
    }
}