package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.blockchain.common.*
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import com.tangem.tap.*
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.isMultiwalletAllowed
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                        scope.launch {
                            val result = tangemSdkManager.scanNote(
                                FirebaseAnalyticsHandler,
                                R.string.wallet_connect_scan_card_message
                            )
                            withContext(Dispatchers.Main) {
                                when (result) {
                                    is CompletionResult.Success -> {
                                        val card = result.data.card
                                        val factory =
                                            store.state.globalState.tapWalletManager.walletManagerFactory

                                        val walletManager = if (card.isMultiwalletAllowed) {
                                            if (currenciesRepository.loadCardCurrencies(card.cardId)?.blockchains?.contains(
                                                    Blockchain.Ethereum) == true
                                            ) {
                                                factory.makeWalletManagerForApp(result.data.card,
                                                    Blockchain.Ethereum)
                                            } else {
                                                factory.makeWalletManagerForApp(result.data.card,
                                                    Blockchain.Ethereum)?.also {
                                                    currenciesRepository.saveAddedBlockchain(card.cardId,
                                                        Blockchain.Ethereum)
                                                }
                                            }
                                        } else {
                                            null
                                        }
                                        if (walletManager == null) {
                                            store.dispatchOnMain(WalletConnectAction.UnsupportedCard)
                                            return@withContext
                                        };

                                        val key = walletManager.wallet.publicKey
                                        store.dispatchOnMain(WalletConnectAction.OpenSession(
                                            wcUri = action.wcUri,
                                            wallet = WalletForSession(
                                                card.cardId, key.toHexString(),
                                                isTestNet = false
                                            ),
                                        ))
                                    }
                                    is CompletionResult.Failure ->
                                        store.dispatchOnMain(WalletConnectAction.FailureEstablishingSession)
                                }
                            }
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
}
