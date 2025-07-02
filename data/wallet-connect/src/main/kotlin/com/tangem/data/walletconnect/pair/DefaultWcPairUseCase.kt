package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.reown.walletkit.client.Wallet
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.data.walletconnect.utils.WcSdkSessionConverter
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.walletconnect.model.*
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber

val unsupportedDApps = listOf("dYdX", "dYdX v4", "Apex Pro", "The Sandbox")

@Suppress("LongParameterList")
internal class DefaultWcPairUseCase @AssistedInject constructor(
    private val sessionsManager: WcSessionsManager,
    private val associateNetworksDelegate: AssociateNetworksDelegate,
    private val caipNamespaceDelegate: CaipNamespaceDelegate,
    private val sdkDelegate: WcPairSdkDelegate,
    private val blockAidVerifier: BlockAidVerifier,
    private val analytics: AnalyticsEventHandler,
    @Assisted private val pairRequest: WcPairRequest,
) : WcPairUseCase {

    private val onCallTerminalAction = Channel<TerminalAction>()

    override operator fun invoke(): Flow<WcPairState> {
        val (uri: String, source: WcPairRequest.Source) = pairRequest
        return flow {
            Timber.tag(WC_TAG).i("start pair flow $pairRequest")
            analytics.send(WcAnalyticEvents.NewPairInitiated(source))
            emit(WcPairState.Loading)

            val sdkSessionProposal = sdkDelegate.pair(uri)
                .onLeft {
                    Timber.tag(WC_TAG).e(it, "Failed to call pair $pairRequest")
                    emit(WcPairState.Error(it))
                }
                .getOrNull() ?: return@flow

            // check unsupported dApps, just local constant for now, finish if unsupported
            if (sdkSessionProposal.name in unsupportedDApps) {
                Timber.tag(WC_TAG).i("Unsupported DApp ${sdkSessionProposal.name}")
                val error = WcPairState.Error(WcPairError.UnsupportedDomain)
                emit(error)
                return@flow
            }

            val proposalState = buildProposalState(sdkSessionProposal)
                .onLeft {
                    analytics.send(WcAnalyticEvents.PairFailed)
                    emit(WcPairState.Error(it))
                }
                .getOrNull() ?: return@flow
            emit(proposalState)

            // wait first terminal action and continue WC pair flow
            Timber.tag(WC_TAG).i("pair wait terminal action ${sdkSessionProposal.name}")
            val terminalAction = onCallTerminalAction.receiveAsFlow().first()
            val sessionForApprove: WcSessionApprove? = when (terminalAction) {
                is TerminalAction.Approve -> terminalAction.sessionForApprove
                TerminalAction.Reject -> null
            }
            // finish flow if rejected above
            if (sessionForApprove == null) {
                analytics.send(WcAnalyticEvents.SessionDisconnected(proposalState.dAppSession))
                sdkDelegate.rejectSession(sdkSessionProposal.proposerPublicKey)
                return@flow
            }

            // start flow of approving in wc sdk
            emit(WcPairState.Approving.Loading(sessionForApprove))
            val either = walletKitApproveSession(
                sessionForApprove = sessionForApprove,
                sdkSessionProposal = sdkSessionProposal,
            ).map { settledSession ->
                val newSession = WcSession(
                    wallet = sessionForApprove.wallet,
                    sdkModel = WcSdkSessionConverter.convert(settledSession.session),
                    securityStatus = proposalState.dAppSession.securityStatus,
                    networks = sessionForApprove.network.toSet(),
                )
                sessionsManager.saveSession(newSession)
                analytics.send(
                    WcAnalyticEvents.DAppConnected(
                        proposalState.dAppSession,
                        sessionForApprove,
                    ),
                )
                newSession
            }.onLeft {
                analytics.send(WcAnalyticEvents.DAppConnectionFailed(it.code))
                sdkDelegate.rejectSession(sdkSessionProposal.proposerPublicKey)
                Timber.tag(WC_TAG).e(it, "Failed to approve session ${sdkSessionProposal.name}")
            }
            emit(WcPairState.Approving.Result(sessionForApprove, either))
        }.onCompletion {
            if (it != null) {
                Timber.tag(WC_TAG).e(it, "Completed with error $pairRequest")
            } else {
                Timber.tag(WC_TAG).i("Completed successfully $pairRequest")
            }
        }
    }

    override fun approve(sessionForApprove: WcSessionApprove) {
        analytics.send(WcAnalyticEvents.PairButtonConnect)
        onCallTerminalAction.trySend(TerminalAction.Approve(sessionForApprove))
    }

    override fun reject() {
        analytics.send(WcAnalyticEvents.ButtonCancel(WcAnalyticEvents.ButtonCancel.Type.Connection))
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
            Timber.tag(WC_TAG).e(it, "Failed to verify DApp ${sessionProposal.name}")
            CheckDAppResult.FAILED_TO_VERIFY
        }
        val requestedNetworks = proposalNetwork
            .values.map { it.available.plus(it.required) }.flatten().toSet()
        analytics.send(
            WcAnalyticEvents.PairRequested(
                network = requestedNetworks,
                verificationInfo.name,
            ),
        )
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

    private sealed interface TerminalAction {
        data class Approve(val sessionForApprove: WcSessionApprove) : TerminalAction
        data object Reject : TerminalAction
    }

    @AssistedFactory
    interface Factory : WcPairUseCase.Factory {
        override fun create(pairRequest: WcPairRequest): DefaultWcPairUseCase
    }
}