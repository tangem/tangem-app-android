package com.tangem.domain.walletconnect

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest

sealed class WcAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Wallet Connect", event = event, params = params) {

    class NewPairInitiated(source: WcPairRequest.Source) : WcAnalyticEvents(
        event = "Session Initiated",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to when (source) {
                WcPairRequest.Source.QR -> "QR"
                WcPairRequest.Source.DEEPLINK -> "DeepLink"
                WcPairRequest.Source.CLIPBOARD -> "Clipboard"
                WcPairRequest.Source.ETC -> "etc"
            },
        ),
    )

    data object PairButtonConnect : WcAnalyticEvents(
        event = "Button - Connect",
    )

    class PairRequested(
        network: Set<Network>,
        domainVerification: String,
    ) : WcAnalyticEvents(
        event = "dApp Connection Requested",
        params = mapOf(
            NETWORKS to network.joinToString(",") { it.name },
            DOMAIN_VERIFICATION to domainVerification,
        ),
    )

    data object PairFailed : WcAnalyticEvents(
        event = "Session Failed",
    )

    class DAppConnected(
        sessionProposal: WcSessionProposal,
        sessionForApprove: WcSessionApprove,
    ) : WcAnalyticEvents(
        event = "dApp Connected",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to sessionProposal.dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to sessionProposal.dAppMetaData.url,
            AnalyticsParam.Key.BLOCKCHAIN to sessionForApprove.network.joinToString(",") { it.name },
        ),
    )

    class DAppConnectionFailed(
        errorCode: String,
    ) : WcAnalyticEvents(
        event = "dApp Connection Failed",
        params = mapOf(
            AnalyticsParam.Key.ERROR_CODE to errorCode,
        ),
    )

    class SessionDisconnected(sessionProposal: WcSessionProposal) : WcAnalyticEvents(
        event = "dApp Disconnected",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to sessionProposal.dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to sessionProposal.dAppMetaData.url,
        ),
    )

    class SignatureRequestReceived(
        session: WcSession,
        rawRequest: WcSdkSessionRequest,
        network: Network,
    ) : WcAnalyticEvents(
        event = "Signature Request Received",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to session.sdkModel.appMetaData.name,
            AnalyticsParam.Key.DAPP_URL to session.sdkModel.appMetaData.url,
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.Key.BLOCKCHAIN to network.name,
        ),
    )

    class SignatureRequestHandled(
        session: WcSession,
        rawRequest: WcSdkSessionRequest,
        network: Network,
    ) : WcAnalyticEvents(
        event = "Signature Request Handled",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to session.sdkModel.appMetaData.name,
            AnalyticsParam.Key.DAPP_URL to session.sdkModel.appMetaData.url,
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.Key.BLOCKCHAIN to network.name,
        ),
    )

    class SignatureRequestFailed(
        session: WcSession,
        rawRequest: WcSdkSessionRequest,
        network: Network,
        errorCode: String,
    ) : WcAnalyticEvents(
        event = "Signature Request Failed",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to session.sdkModel.appMetaData.name,
            AnalyticsParam.Key.DAPP_URL to session.sdkModel.appMetaData.url,
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.Key.BLOCKCHAIN to network.name,
            AnalyticsParam.Key.ERROR_CODE to errorCode,
        ),
    )

    class ButtonSign(
        rawRequest: WcSdkSessionRequest,
    ) : WcAnalyticEvents(
        event = "Button - Sign",
        params = mapOf(
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
        ),
    )

    class ButtonCancel(
        type: Type,
    ) : WcAnalyticEvents(
        event = "Button - Sign",
        params = mapOf(
            AnalyticsParam.Key.TYPE to type.type,
        ),
    ) {
        enum class Type(val type: String) {
            Connection("Connection"),
            Sign("Sign"),
        }
    }

    data object ButtonDisconnectAll : WcAnalyticEvents(
        event = "Button - Disconnect All",
    )

    class ButtonDisconnect(
        session: WcSession,
    ) : WcAnalyticEvents(
        event = "Button - Disconnect",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to session.sdkModel.appMetaData.name,
            AnalyticsParam.Key.DAPP_URL to session.sdkModel.appMetaData.url,
        ),
    )

    companion object {
        const val NETWORKS = "Networks"
        const val DOMAIN_VERIFICATION = "Domain Verification"
    }
}