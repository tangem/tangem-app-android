package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.data.walletconnect.utils.toOurModel
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionProposal
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

val unsupportedDApps = listOf("dYdX", "dYdX v4", "Apex Pro", "The Sandbox")

@Suppress("UnusedPrivateMember") // todo(wc) remove after add mapping
internal class DefaultWcPairUseCase(
    private val sessionsManager: WcSessionsManager,
) : WcPairUseCase, WcSdkObserver {

    private val onWalletSelect = Channel<UserWallet>()
    private val onAccountSelect = Channel<Any>()
    private val onCallTerminalAction = Channel<TerminalAction>()
    private val onSessionProposal =
        Channel<Pair<Wallet.Model.SessionProposal, Wallet.Model.VerifyContext>>(Channel.BUFFERED)
    private val onSessionSettleResponse = Channel<Wallet.Model.SettledSessionResponse>(Channel.BUFFERED)

    override fun pairFlow(uri: String, source: WcPairUseCase.Source, selectedWallet: UserWallet): Flow<WcPairState> =
        flow {
            emit(WcPairState.Loading)

            // call sdk.pair and wait result, finish flow on error
            walletKitPair(uri).onLeft { throwable ->
                emit(WcPairState.Error(throwable))
                return@flow
            }
            // wait for sdk onSessionProposal callback
            val (sdkSessionProposal, verifyContext) = onSessionProposal.receiveAsFlow().map { (fist, second) ->
                val ourCopy = WcSdkSessionProposal(
                    name = fist.name,
                    description = fist.description,
                    url = fist.url,
                    proposerPublicKey = fist.proposerPublicKey,
                )
                ourCopy to second
            }.first { (sessionProposal, verifyContext) ->
                true // todo(wc) check verifyContext? compare uri?
            }

            // check unsupported dApps, just local constant for now, finish if unsupported
            if (sdkSessionProposal.name in unsupportedDApps) {
                Timber.i("Unsupported DApp")
                val error = WcPairState
                    .Error(RuntimeException("todo(wc) use domain exception")) // todo(wc) WalletConnectError.UnsupportedDApp
                emit(error)
                return@flow
            }

            // flow that collect terminal and all middle actions
            val actionsFlow = channelFlow<TerminalAction> {
                val userWallet = onWalletSelect.receiveAsFlow()
                    .stateIn(scope = this, started = SharingStarted.Lazily, initialValue = selectedWallet)
                val accountSelect = onAccountSelect.receiveAsFlow()
                    .stateIn(scope = this, started = SharingStarted.Lazily, initialValue = Any())
                // combine all middle variables and emit new ProposalState to main FlowCollector
                combine(accountSelect, userWallet) { account, selectedWallet ->
                    buildProposalState(sdkSessionProposal, verifyContext, account, selectedWallet)
                }.onEach { newProposalState -> this@flow.emit(newProposalState) }
                    .launchIn(this)

                // collect terminal action and emit to this inner channelFlow, unlike combine above
                onCallTerminalAction.receiveAsFlow()
                    .onEach { this.channel.send(it) }
                    .launchIn(this)
            }

            // wait first terminal action and continue WC pair flow
            val sessionForApprove: WcSessionProposal = when (val terminalAction = actionsFlow.first()) {
                is TerminalAction.Approve -> terminalAction.sessionForApprove
                TerminalAction.Reject -> {
                    // non suspending WalletKit.rejectSession call
                    rejectSession(sdkSessionProposal)
                    return@flow
                }
            }

            // start flow of approving in wc sdk
            emit(WcPairState.Approving.Loading(sessionForApprove))
            fun mapResult(either: Either<Throwable, WcSession>) =
                WcPairState.Approving.Result(sessionForApprove, either)
            // call sdk approve and wait result
            walletKitApproveSession(sessionForApprove).onLeft { emit(mapResult(it.left())) }.onRight {
                // wait for sdk callback
                val result = when (val settledSession = onSessionSettleResponse.receiveAsFlow().first()) {
                    is Wallet.Model.SettledSessionResponse.Error -> mapResult(
                        // WalletConnectError.ExternalApprovalError(settledSession.errorMessage) todo(wc)
                        RuntimeException("todo(wc) use domain exception").left(),
                    )

                    is Wallet.Model.SettledSessionResponse.Result -> {
                        val newSession = settledSession.session.toDomain(sessionForApprove.wallet)
                        sessionsManager.saveSessions(sessionForApprove.wallet.walletId, newSession)
                        mapResult(newSession.right())
                    }
                }
                emit(result)
            }
        }

    override fun onWalletSelect(selectedWallet: UserWallet) {
        onWalletSelect.trySend(selectedWallet)
    }

    override fun onAccountSelect(account: Any) {
        onAccountSelect.trySend(account)
    }

    override fun approve(sessionForApprove: WcSessionProposal) {
        onCallTerminalAction.trySend(TerminalAction.Approve(sessionForApprove))
    }

    override fun reject() {
        onCallTerminalAction.trySend(TerminalAction.Reject)
    }

    override fun onSessionProposal(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        // Triggered when wallet receives the session proposal sent by a Dapp
        Timber.i("sessionProposal: $sessionProposal")
        onSessionProposal.trySend(sessionProposal to verifyContext)
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        // Triggered when wallet receives the session settlement response from Dapp
        Timber.i("onSessionSettleResponse: $settleSessionResponse")
        onSessionSettleResponse.trySend(settleSessionResponse)
    }

    private suspend fun walletKitPair(uri: String): Either<Throwable, Unit> =
        suspendCancellableCoroutine { continuation ->
            WalletKit.pair(
                params = Wallet.Params.Pair(uri),
                onSuccess = {
                    Timber.i("Paired successfully: $it")
                    continuation.resume(Unit.right())
                },
                onError = {
                    Timber.e("Error while pairing: $it")
                    continuation.resume(it.throwable.left())
                },
            )
        }

    private suspend fun walletKitApproveSession(sessionForApprove: WcSessionProposal): Either<Throwable, Unit> =
        suspendCancellableCoroutine { continuation ->
            WalletKit.approveSession(
                params = TODO("wc sdk model"),
                onSuccess = {
                    Timber.i("Approved successfully: $it")
                    continuation.resume(Unit.right())
                },
                onError = {
                    Timber.e("Error while approving: $it")
                    continuation.resume(it.throwable.left())
                },
            )
        }

    private fun rejectSession(sessionProposal: WcSdkSessionProposal) {
        WalletKit.rejectSession(
            params = Wallet.Params.SessionReject(
                proposerPublicKey = sessionProposal.proposerPublicKey,
                reason = "",
            ),
            onSuccess = {
                Timber.i("Rejected successfully: $it")
            },
            onError = {
                Timber.e("Error while rejecting: $it")
            },
        )
    }

    private fun buildProposalState(
        sessionProposal: WcSdkSessionProposal,
        verifyContext: Wallet.Model.VerifyContext,
        selectedAccount: Any,
        userWallet: UserWallet,
    ): WcPairState.Proposal {
// [REDACTED_TODO_COMMENT]
// [REDACTED_TODO_COMMENT]
        val mock: WcSessionProposal = TODO()
        return WcPairState.Proposal(mock, Any())
    }

    private fun Wallet.Model.Session.toDomain(userWallet: UserWallet): WcSession = WcSession(
        userWalletId = userWallet.walletId,
        sdkModel = this.toOurModel(),
    )

    private sealed interface TerminalAction {
        data class Approve(val sessionForApprove: WcSessionProposal) : TerminalAction
        data object Reject : TerminalAction
    }
}
