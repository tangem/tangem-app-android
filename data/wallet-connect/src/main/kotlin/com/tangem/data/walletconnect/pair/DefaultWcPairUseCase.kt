package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.reown.walletkit.client.Wallet
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.data.walletconnect.utils.getDappOriginUrl
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.model.*
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.walletconnect.usecase.pair.WcPairUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.joda.time.DateTime
import org.joda.time.Duration
import timber.log.Timber
import java.net.URI

@Suppress("LongParameterList")
internal class DefaultWcPairUseCase @AssistedInject constructor(
    private val associateNetworksDelegate: AssociateNetworksDelegate,
    private val caipNamespaceDelegate: CaipNamespaceDelegate,
    private val sdkDelegate: WcPairSdkDelegate,
    private val blockAidVerifier: BlockAidVerifier,
    private val analytics: AnalyticsEventHandler,
    @Assisted private val pairRequest: WcPairRequest,
) : WcPairUseCase {

    private val onCallTerminalAction = Channel<TerminalAction>()

    @Suppress("LongMethod")
    override operator fun invoke(): Flow<WcPairState> {
        val (uri: String, source: WcPairRequest.Source) = pairRequest
        return flow {
            Timber.tag(WC_TAG).i("start pair flow $pairRequest")
            analytics.send(WcAnalyticEvents.NewPairInitiated(source))
            emit(WcPairState.Loading)

            val pairResult = sdkDelegate.pair(uri)
                .onLeft {
                    Timber.tag(WC_TAG).e(it, "Failed to call pair $pairRequest")
                    analytics.send(
                        WcAnalyticEvents.PairFailed(
                            errorCode = it.code,
                            errorMessage = it.message,
                        ),
                    )
                    emit(WcPairState.Error(it))
                }
                .getOrNull() ?: return@flow
            val (sdkSessionProposal, sdkVerifyContext) = pairResult

            // check unsupported dApps, just local constant for now, finish if unsupported
            if (UnsupportedDApps.list.any { sdkSessionProposal.url.contains(it, ignoreCase = true) }) {
                Timber.tag(WC_TAG).i("Unsupported DApp ${sdkSessionProposal.name}")
                val error = WcPairState.Error(WcPairError.UnsupportedDApp(sdkSessionProposal.name))
                emit(error)
                return@flow
            }

            val dAppUri = URI(sdkSessionProposal.url)
            if (dAppUri.host.isNullOrEmpty()) {
                emit(WcPairState.Error(WcPairError.InvalidDomainURL))
                return@flow
            }

            val proposalState = buildProposalState(sdkSessionProposal, sdkVerifyContext)
                .onLeft {
                    analytics.send(
                        WcAnalyticEvents.PairFailed(
                            errorCode = it.code,
                            errorMessage = it.message,
                        ),
                    )
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
                sdkDelegate.rejectSession(sdkSessionProposal.proposerPublicKey)
                return@flow
            }

            // start flow of approving in wc sdk
            emit(WcPairState.Approving.Loading(sessionForApprove))

            val connectingTime = DateTime.now().millis
            val expiredTime = connectingTime + Duration
                .standardMinutes(PENDING_SESSION_EXPIRED_DURATION_MIN)
                .millis
            val sessionDTO = WcSessionDTO(
                topic = "",
                walletId = sessionForApprove.wallet.walletId,
                url = sdkVerifyContext.getDappOriginUrl(),
                securityStatus = proposalState.dAppSession.securityStatus,
                connectingTime = connectingTime,
            )
            val pendingSessionForSave = WcPendingApprovalSessionDTO(
                pairingTopic = sdkSessionProposal.pairingTopic,
                session = sessionDTO,
                expiredTime = expiredTime,
            )

            val either = walletKitApproveSession(
                pendingSessionForSave = pendingSessionForSave,
                sessionForApprove = sessionForApprove,
                sdkSessionProposal = sdkSessionProposal,
            ).map { settledSession ->
                analytics.send(
                    WcAnalyticEvents.DAppConnected(
                        sessionProposal = proposalState.dAppSession,
                        sessionForApprove = sessionForApprove,
                        securityStatus = proposalState.dAppSession.securityStatus,
                    ),
                )
                proposalState.dAppSession.dAppMetaData
            }.onLeft {
                analytics.send(
                    WcAnalyticEvents.DAppConnectionFailed(
                        errorCode = it.code,
                        errorMessage = it.message,
                    ),
                )
                sdkDelegate.rejectSession(sdkSessionProposal.proposerPublicKey)
                Timber.tag(WC_TAG).e(it, "Failed to approve session ${sdkSessionProposal.name}")
            }
            emit(WcPairState.Approving.Result(sessionForApprove, either))
        }
            .catch {
                val pairError: WcPairError = when (it) {
                    is TimeoutCancellationException -> WcPairError.TimeoutException(it.message.orEmpty())
                    else -> WcPairError.Unknown(it.message.orEmpty())
                }
                emit(WcPairState.Error(pairError))
            }
            .onCompletion {
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
        pendingSessionForSave: WcPendingApprovalSessionDTO,
        sessionForApprove: WcSessionApprove,
        sdkSessionProposal: Wallet.Model.SessionProposal,
    ): Either<WcPairError, Unit> = try {
        val namespaces = caipNamespaceDelegate.associate(
            sdkSessionProposal,
            sessionForApprove,
        )
        val sessionApprove = Wallet.Params.SessionApprove(
            proposerPublicKey = sdkSessionProposal.proposerPublicKey,
            namespaces = namespaces,
        )
        sdkDelegate.approve(pendingSessionForSave, sessionApprove)
    } catch (e: Throwable) {
        Timber.tag(WC_TAG).e(e, "Failed to sdk approve session $pairRequest")
        WcPairError.ApprovalFailed(e.message.orEmpty()).left()
    }

    private suspend fun buildProposalState(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext,
    ): Either<WcPairError, WcPairState.Proposal> = runCatching {
        val proposalNetwork = associateNetworksDelegate.associate(sessionProposal)
        val verificationInfo = when {
            verifyContext.validation == Wallet.Model.Validation.INVALID -> CheckDAppResult.UNSAFE
            verifyContext.isScam == true -> CheckDAppResult.UNSAFE
            else -> blockAidVerifier.verifyDApp(DAppData(sessionProposal.url)).getOrElse {
                Timber.tag(WC_TAG).e(it, "Failed to verify DApp ${sessionProposal.name}")
                CheckDAppResult.FAILED_TO_VERIFY
            }
        }
        val requestedNetworks = proposalNetwork
            .values.map { it.available.plus(it.required) }.flatten().toSet()
        analytics.send(
            WcAnalyticEvents.PairRequested(
                dAppName = sessionProposal.name,
                dAppUrl = sessionProposal.url,
                network = requestedNetworks,
                domainVerification = verificationInfo,
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
    }.fold(
        onSuccess = { it.right() },
        onFailure = {
            when (it) {
                is WcPairError -> it.left()
                else -> WcPairError.Unknown(it.localizedMessage.orEmpty()).left()
            }
        },
    )

    private companion object {
        const val PENDING_SESSION_EXPIRED_DURATION_MIN = 15L
    }

    private sealed interface TerminalAction {
        data class Approve(val sessionForApprove: WcSessionApprove) : TerminalAction
        data object Reject : TerminalAction
    }

    @AssistedFactory
    interface Factory : WcPairUseCase.Factory {
        override fun create(pairRequest: WcPairRequest): DefaultWcPairUseCase
    }
}