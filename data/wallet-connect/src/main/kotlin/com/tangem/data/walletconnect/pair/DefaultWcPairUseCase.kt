package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.utils.WcSdkSessionConverter
import com.tangem.domain.blockaid.BlockAidVerifier
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
import timber.log.Timber

val unsupportedDApps = listOf("dYdX", "dYdX v4", "Apex Pro", "The Sandbox")

internal class DefaultWcPairUseCase(
    private val sessionsManager: WcSessionsManager,
    private val associateNetworksDelegate: AssociateNetworksDelegate,
    private val caipNamespaceDelegate: CaipNamespaceDelegate,
    private val sdkDelegate: WcPairSdkDelegate,
    private val blockAidVerifier: BlockAidVerifier,
) : WcPairUseCase {

    private val onCallTerminalAction = Channel<TerminalAction>()

    override fun pairFlow(uri: String, source: WcPairUseCase.Source): Flow<WcPairState> {
        return flow {
            emit(WcPairState.Loading)

            val sdkSessionProposal = sdkDelegate.pair(uri)
                .onLeft { emit(WcPairState.Error(it)) }
                .getOrNull() ?: return@flow

            // check unsupported dApps, just local constant for now, finish if unsupported
            if (sdkSessionProposal.name in unsupportedDApps) {
                Timber.i("Unsupported DApp")
                val error = WcPairState.Error(WcPairError.UnsupportedDApp)
                emit(error)
                return@flow
            }

            val proposalState = buildProposalState(sdkSessionProposal)
                .onLeft { emit(WcPairState.Error(it)) }
                .getOrNull() ?: return@flow
            emit(proposalState)

            // wait first terminal action and continue WC pair flow
            val terminalAction = onCallTerminalAction.receiveAsFlow().first()
            val sessionForApprove: WcSessionApprove? = when (terminalAction) {
                is TerminalAction.Approve -> terminalAction.sessionForApprove
                TerminalAction.Reject -> null
            }
            // finish flow if rejected above
            if (sessionForApprove == null) {
                sdkDelegate.rejectSession(sdkSessionProposal.proposerPublicKey)
                return@flow
            }

            // start flow of approving in wc sdk
            emit(WcPairState.Approving.Loading(sessionForApprove))
            val either = walletKitApproveSession(
                sessionForApprove = sessionForApprove,
                sdkSessionProposal = sdkSessionProposal,
            ).map { settledSession ->
                val newSession = settledSession.session.toDomain(
                    wallet = sessionForApprove.wallet,
                    securityStatus = proposalState.dAppSession.securityStatus,
                )
                sessionsManager.saveSession(newSession)
                newSession
            }
            emit(WcPairState.Approving.Result(sessionForApprove, either))
        }
    }

    override fun approve(sessionForApprove: WcSessionApprove) {
        onCallTerminalAction.trySend(TerminalAction.Approve(sessionForApprove))
    }

    override fun reject() {
        onCallTerminalAction.trySend(TerminalAction.Reject)
    }

    private suspend fun walletKitApproveSession(
        sessionForApprove: WcSessionApprove,
        sdkSessionProposal: Wallet.Model.SessionProposal,
    ): Either<WcPairError, Wallet.Model.SettledSessionResponse.Result> {
        val namespaces = caipNamespaceDelegate.associate(
            sdkSessionProposal,
            sessionForApprove.wallet,
            sessionForApprove.network,
        )
        val sessionApprove = Wallet.Params.SessionApprove(
            proposerPublicKey = sdkSessionProposal.proposerPublicKey,
            namespaces = namespaces,
        )
        return sdkDelegate.approve(sessionApprove)
    }

    private suspend fun buildProposalState(
        sessionProposal: Wallet.Model.SessionProposal,
    ): Either<WcPairError, WcPairState.Proposal> = runCatching {
        val proposalNetwork = associateNetworksDelegate.associate(sessionProposal)
        val verificationInfo = blockAidVerifier.verifyDApp(DAppData(sessionProposal.url)).getOrElse {
            Timber.e("Failed to verify DApp: ${it.localizedMessage}")
            CheckDAppResult.FAILED_TO_VERIFY
        }
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
            securityStatus = verificationInfo,
        )
        WcPairState.Proposal(dAppSession)
    }.fold(onSuccess = { it.right() }, onFailure = {
        when (it) {
            is WcPairError -> it.left()
            else -> WcPairError.Unknown(it.localizedMessage.orEmpty()).left()
        }
    },)

    private fun Wallet.Model.Session.toDomain(wallet: UserWallet, securityStatus: CheckDAppResult): WcSession {
        return WcSession(
            wallet = wallet,
            sdkModel = WcSdkSessionConverter.convert(this),
            securityStatus = securityStatus,
        )
    }

    private sealed interface TerminalAction {
        data class Approve(val sessionForApprove: WcSessionApprove) : TerminalAction
        data object Reject : TerminalAction
    }
}