package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.data.walletconnect.utils.WcSdkSessionConverter
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

val unsupportedDApps = listOf("dYdX", "dYdX v4", "Apex Pro", "The Sandbox")

internal class DefaultWcPairUseCase(
    private val sessionsManager: WcSessionsManager,
    private val associateNetworksDelegate: AssociateNetworksDelegate,
    private val caipNamespaceDelegate: CaipNamespaceDelegate,
) : WcPairUseCase, WcSdkObserver {

    private val onCallTerminalAction = Channel<TerminalAction>()
    private val onSessionProposal =
        Channel<Pair<Wallet.Model.SessionProposal, Wallet.Model.VerifyContext>>(Channel.BUFFERED)
    private val onSessionSettleResponse = Channel<Wallet.Model.SettledSessionResponse>(Channel.BUFFERED)

    override fun pairFlow(uri: String, source: WcPairUseCase.Source): Flow<WcPairState> {
        return flow {
            emit(WcPairState.Loading)

            // call sdk.pair and wait result, finish flow on error
            walletKitPair(uri).onLeft { throwable ->
                emit(WcPairState.Error(WcPairError.Unknown(throwable.localizedMessage.orEmpty())))
                return@flow
            }
            // wait for sdk onSessionProposal callback
            val (sdkSessionProposal, verifyContext) = onSessionProposal.receiveAsFlow()
                .first { (sessionProposal, verifyContext) ->
                    true // todo(wc) check verifyContext? compare uri?
                }

            // check unsupported dApps, just local constant for now, finish if unsupported
            if (sdkSessionProposal.name in unsupportedDApps) {
                Timber.i("Unsupported DApp")
                val error = WcPairState.Error(WcPairError.UnsupportedDApp)
                emit(error)
                return@flow
            }

            val proposalState = buildProposalState(sdkSessionProposal)
                .fold(ifLeft = { WcPairState.Error(it) }, ifRight = { it })
            emit(proposalState)

            // wait first terminal action and continue WC pair flow
            val terminalAction = onCallTerminalAction.receiveAsFlow().first()
            val sessionForApprove: WcSessionApprove? = when (terminalAction) {
                is TerminalAction.Approve -> terminalAction.sessionForApprove
                TerminalAction.Reject -> {
                    // non suspending WalletKit.rejectSession call
                    rejectSession(sdkSessionProposal.proposerPublicKey)
                    null
                }
            }
            // finish flow if rejected above
            sessionForApprove ?: return@flow

            // start flow of approving in wc sdk
            emit(WcPairState.Approving.Loading(sessionForApprove))
            // call sdk approve and wait result
            val either = walletKitApproveSession(
                sessionForApprove = sessionForApprove,
                sdkSessionProposal = sdkSessionProposal,
            ).fold(
                ifLeft = { WcPairError.ExternalApprovalError(it.localizedMessage.orEmpty()).left() },
                ifRight = {
                    when (val settledSession = onSessionSettleResponse.receiveAsFlow().first()) {
                        is Wallet.Model.SettledSessionResponse.Error -> WcPairError.ExternalApprovalError(
                            settledSession.errorMessage,
                        ).left()

                        is Wallet.Model.SettledSessionResponse.Result -> {
                            val newSession = settledSession.session.toDomain(sessionForApprove.wallet)
                            sessionsManager.saveSession(newSession)
                            newSession.right()
                        }
                    }
                },
            )
            emit(WcPairState.Approving.Result(sessionForApprove, either))
        }
    }

    override fun approve(sessionForApprove: WcSessionApprove) {
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

    private suspend fun walletKitApproveSession(
        sessionForApprove: WcSessionApprove,
        sdkSessionProposal: Wallet.Model.SessionProposal,
    ): Either<Throwable, Unit> {
        val namespaces = caipNamespaceDelegate.associate(
            sdkSessionProposal,
            sessionForApprove.wallet,
            sessionForApprove.network.map { it.network },
        )
        val sessionApprove = Wallet.Params.SessionApprove(
            proposerPublicKey = sdkSessionProposal.proposerPublicKey,
            namespaces = namespaces,
        )
        return suspendCancellableCoroutine { continuation ->
            WalletKit.approveSession(
                params = sessionApprove,
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
    }

    private fun rejectSession(proposerPublicKey: String) {
        WalletKit.rejectSession(
            params = Wallet.Params.SessionReject(
                proposerPublicKey = proposerPublicKey,
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

    private suspend fun buildProposalState(
        sessionProposal: Wallet.Model.SessionProposal,
    ): Either<WcPairError, WcPairState.Proposal> = runCatching {
        val proposalNetwork = associateNetworksDelegate.associate(sessionProposal)
        val appMetaData = WcAppMetaData(
            name = sessionProposal.name,
            description = sessionProposal.description,
            url = sessionProposal.url,
            icons = sessionProposal.icons.map { it.toString() },
            redirect = sessionProposal.redirect,
        )
        val dAppSession = WcSessionProposal(
            dAppMetaData = appMetaData,
            proposalNetwork = proposalNetwork,
            securityStatus = Any(),
        )
        WcPairState.Proposal(dAppSession)
    }.fold(onSuccess = { it.right() }, onFailure = {
        when (it) {
            is WcPairError -> it.left()
            else -> WcPairError.Unknown(it.localizedMessage.orEmpty()).left()
        }
    },)

    private fun Wallet.Model.Session.toDomain(wallet: UserWallet): WcSession = WcSession(
        wallet = wallet,
        sdkModel = WcSdkSessionConverter.convert(this),
    )

    private sealed interface TerminalAction {
        data class Approve(val sessionForApprove: WcSessionApprove) : TerminalAction
        data object Reject : TerminalAction
    }
}