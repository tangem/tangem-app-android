package com.tangem.tap.domain.walletconnect3

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.domain.walletconnect2.data.DefaultLegacyWalletConnectRepository.Companion.unsupportedDApps
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction.OpenSession.SourceType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber

interface WcPairUseCase {

    fun pairFlow(uri: String, source: SourceType, selectedWallet: UserWallet): Flow<WcPairState>

    // todo(wc) should map for all user wallets without this action in domain layer?
    fun onWalletSelect(selectedWallet: UserWallet)
    // todo(wc) accounts in future
    fun onAccountSelect(account: Any)

    fun approve(sessionForApprove: WcSessionProposal)
    fun reject()
}

sealed interface WcPairState {
    data object Loading : WcPairState
    data class Error(val throwable: Throwable) : WcPairState
    data class Proposal(
        val dAppSession: WcSessionProposal,
        val securityStatus: Any,
    ) : WcPairState

    sealed interface Approving : WcPairState {
        val session: WcSessionProposal

        data class Loading(override val session: WcSessionProposal) : Approving
        data class Result(
            override val session: WcSessionProposal,
            val result: Either<Throwable, WcSession>,
        ) : Approving
    }
}

// todo(wc) create our clone model?
typealias WcSdkSessionProposal = Wallet.Model.SessionProposal

data class WcSessionProposal(
    val dAppInfo: WcSdkSessionProposal,

    // todo(wc) should map for all user wallets?
    val wallet: UserWallet,
    val missingChains: List<Blockchain>,
    val availableChains: List<Blockchain>,
    val notAddedChains: List<Blockchain>,
)

internal class DefaultWcPairUseCase(
    private val sessionsManager: WcSessionsManager,
) : WcPairUseCase, WcSdkObserver {

    private val onWalletSelect = MutableSharedFlow<UserWallet>()
    private val onAccountSelect = MutableSharedFlow<Any>()
    private val onCallTerminalAction = MutableSharedFlow<TerminalAction>()
    private val onSessionProposal = MutableSharedFlow<Pair<WcSdkSessionProposal, Wallet.Model.VerifyContext>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val onSessionSettleResponse = MutableSharedFlow<Wallet.Model.SettledSessionResponse>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun pairFlow(uri: String, source: SourceType, selectedWallet: UserWallet): Flow<WcPairState> = flow {
        // clear old onSessionProposal callback, just in case
        onSessionProposal.resetReplayCache()
        emit(WcPairState.Loading)

        // call sdk.pair and wait result, finish flow on error
        walletKitPair(uri).first().onLeft { throwable ->
            emit(WcPairState.Error(throwable))
            return@flow
        }
        // wait for sdk onSessionProposal callback
        val (sdkSessionProposal, verifyContext) = onSessionProposal.first { (sessionProposal, verifyContext) ->
            true // todo(wc) check verifyContext? compare uri?
        }

        // check unsupported dApps, just local constant for now, finish if unsupported
        if (sdkSessionProposal.name in unsupportedDApps) {
            Timber.i("Unsupported DApp")
            emit(WcPairState.Error(WalletConnectError.UnsupportedDApp))
            return@flow
        }

        // emit first dApp Proposal state
        emit(buildProposalState(sdkSessionProposal, verifyContext, selectedWallet))

        // collecting all middle action and wait for final approve or reject dApp connection
        val terminalAction = channelFlow<TerminalAction> {
            // update state on new wallet selected
            onWalletSelect.onEach {
                this@flow.emit(buildProposalState(sdkSessionProposal, verifyContext, it))
            }.collect()
            // update state on account changed(in future)
            onAccountSelect.onEach {}.collect()

            // any terminal action finish this channelFlow
            onCallTerminalAction.onEach { this.channel.send(it) }.collect()
        }.first()

        val sessionForApprove: WcSessionProposal = when (terminalAction) {
            TerminalAction.Reject -> {
                // non suspending WalletKit.rejectSession call
                rejectSession(sdkSessionProposal)
                return@flow
            }
            is TerminalAction.Approve -> terminalAction.sessionForApprove
        }

        // clear old onSessionSettleResponse callback, just in case
        onSessionSettleResponse.resetReplayCache()

        // start flow of approving in wc sdk
        emit(WcPairState.Approving.Loading(sessionForApprove))
        fun mapResult(either: Either<Throwable, WcSession>) = WcPairState.Approving.Result(sessionForApprove, either)
        // call sdk approve and wait result
        walletKitApproveSession(sessionForApprove).first()
            .onLeft { emit(mapResult(it.left())) }
            .onRight {
                // wait for sdk callback
                val result = when (val settledSession = onSessionSettleResponse.first()) {
                    is Wallet.Model.SettledSessionResponse.Error -> mapResult(
                        WalletConnectError.ExternalApprovalError(
                            settledSession.errorMessage,
                        ).left(),
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
        onWalletSelect.tryEmit(selectedWallet)
    }

    override fun onAccountSelect(account: Any) {
        TODO("Not yet implemented")
    }

    override fun approve(sessionForApprove: WcSessionProposal) {
        onCallTerminalAction.tryEmit(TerminalAction.Approve(sessionForApprove))
    }

    override fun reject() {
        onCallTerminalAction.tryEmit(TerminalAction.Reject)
    }

    override fun onSessionProposal(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        // Triggered when wallet receives the session proposal sent by a Dapp
        Timber.i("sessionProposal: $sessionProposal")
        onSessionProposal.tryEmit(sessionProposal to verifyContext)
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        // Triggered when wallet receives the session settlement response from Dapp
        Timber.i("onSessionSettleResponse: $settleSessionResponse")
        onSessionSettleResponse.tryEmit(settleSessionResponse)
    }

    private fun walletKitPair(uri: String): Flow<Either<Throwable, Unit>> {
        return callbackFlow {
            WalletKit.pair(
                params = Wallet.Params.Pair(uri),
                onSuccess = {
                    Timber.i("Paired successfully: $it")
                    channel.trySend(Unit.right())
                    channel.close()
                },
                onError = {
                    Timber.e("Error while pairing: $it")
                    channel.trySend(it.throwable.left())
                    channel.close()
                },
            )
            awaitClose()
        }
    }

    private fun walletKitApproveSession(sessionForApprove: WcSessionProposal): Flow<Either<Throwable, Unit>> {
        return callbackFlow {
            WalletKit.approveSession(
                params = TODO("wc sdk model"),
                onSuccess = {
                    Timber.i("Approved successfully: $it")
                    channel.trySend(Unit.right())
                    channel.close()
                },
                onError = {
                    Timber.e("Error while approving: $it")
                    channel.trySend(it.throwable.left())
                    channel.close()
                },
            )
            awaitClose()
        }
    }

    private fun rejectSession(sessionProposal: Wallet.Model.SessionProposal) {
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
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext,
        userWallet: UserWallet,
    ): WcPairState.Proposal {
        // todo(wc) a lot of mapping from Wallet.Model.SessionProposal to our Blockchain, Network, ect
        // todo(wc) check security status by Wallet.Model.VerifyContext or Blockaid, don't know for now
        val mock = WcSessionProposal(
            dAppInfo = sessionProposal,
            wallet = userWallet,
            missingChains = listOf(Blockchain.Ethereum),
            availableChains = listOf(Blockchain.BitcoinCash, Blockchain.Polygon),
            notAddedChains = listOf(Blockchain.Solana),
        )
        return WcPairState.Proposal(mock, Any())
    }

    private sealed interface TerminalAction {
        data class Approve(val sessionForApprove: WcSessionProposal) : TerminalAction
        data object Reject : TerminalAction
    }
}
