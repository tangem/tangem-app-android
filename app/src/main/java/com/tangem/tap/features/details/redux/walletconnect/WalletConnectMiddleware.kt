package com.tangem.tap.features.details.redux.walletconnect

import androidx.core.os.bundleOf
import com.tangem.blockchain.common.WalletManager
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.domain.walletconnect2.domain.models.Account
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

@Suppress("LargeClass")
class WalletConnectMiddleware {
    private val walletConnectInteractor: WalletConnectInteractor
        get() = store.inject(DaggerGraphState::walletConnectInteractor)
    private val walletConnectRepository: WalletConnectRepository
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
                if (!action.wcUri.isNullOrBlank()) {
                    store.dispatchOnMain(WalletConnectAction.OpenSession(action.wcUri))
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
                    store.dispatchOnMain(
                        NavigationAction.NavigateTo(
                            screen = AppScreen.QrScanning,
                            bundle = bundleOf(
                                QrScanningRouter.SOURCE_KEY to SourceType.WALLET_CONNECT,
                            ),
                        ),
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
        }
    }

    private suspend fun getWalletManagers(): List<WalletManager> {
        val walletManagerFacade = store.inject(DaggerGraphState::walletManagersFacade)
        val userWallet = userWalletsListManager.selectedUserWalletSync ?: return emptyList()

        return walletManagerFacade.getStoredWalletManagers(userWallet.walletId)
    }

    private fun isWalletConnectUri(uri: String): Boolean {
        return walletConnectInteractor.isWalletConnectUri(uri)
    }
}