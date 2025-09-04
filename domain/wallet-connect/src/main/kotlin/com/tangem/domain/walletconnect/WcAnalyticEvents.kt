package com.tangem.domain.walletconnect

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.CheckDAppResult.*
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.WcAnalyticEvents.ButtonDisconnectAll.toAnalyticVerificationStatus
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.utils.extensions.mapNotNullValues

sealed class WcAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = WC_CATEGORY_NAME, event = event, params = params) {

    object ScreenOpened : WcAnalyticEvents(event = "WC Screen Opened")
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
        domainVerification: CheckDAppResult,
    ) : WcAnalyticEvents(
        event = "dApp Connection Requested",
        params = mapOf(
            NETWORKS to network.joinToString(",") { it.name },
            DOMAIN_VERIFICATION to domainVerification.toAnalyticVerificationStatus(),
        ),
    )

    class PairFailed(
        errorCode: String,
    ) : WcAnalyticEvents(
        event = "Session Failed",
        params = mapOf(
            AnalyticsParam.Key.ERROR_CODE to errorCode,
        ),
    )

    class DAppConnected(
        sessionProposal: WcSessionProposal,
        sessionForApprove: WcSessionApprove,
        securityStatus: CheckDAppResult,
    ) : WcAnalyticEvents(
        event = "dApp Connected",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to sessionProposal.dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to sessionProposal.dAppMetaData.url,
            AnalyticsParam.Key.BLOCKCHAIN to sessionForApprove.network.joinToString(",") { it.name },
            DOMAIN_VERIFICATION to securityStatus.toAnalyticVerificationStatus(),
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

    class SessionDisconnected(dAppMetaData: WcAppMetaData) : WcAnalyticEvents(
        event = "dApp Disconnected",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to dAppMetaData.url,
        ),
    )

    class TransactionDetailsOpened(
        rawRequest: WcSdkSessionRequest,
        network: Network,
    ) : WcAnalyticEvents(
        event = "Transaction Details Opened",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to rawRequest.dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to rawRequest.dAppMetaData.url,
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.Key.BLOCKCHAIN to network.name,
        ),
    )

    class SignatureRequestReceived(
        rawRequest: WcSdkSessionRequest,
        network: Network,
        emulationStatus: EmulationStatus?,
        securityStatus: CheckDAppResult?,
    ) : WcAnalyticEvents(
        event = "Signature Request Received",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to rawRequest.dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to rawRequest.dAppMetaData.url,
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.Key.BLOCKCHAIN to network.name,
            AnalyticsParam.Key.EMULATION_STATUS to emulationStatus?.status,
            AnalyticsParam.Key.TYPE to securityStatus?.toAnalyticVerificationStatus(),
        ).mapNotNullValues { it.value },
    ) {
        enum class EmulationStatus(val status: String) {
            Emulated("Emulated"),
            Error("Error"),
            CanNotEmulate("Can't emulate"),
        }
    }

    class SignatureRequestHandled(
        rawRequest: WcSdkSessionRequest,
        network: Network,
        securityStatus: CheckDAppResult,
    ) : WcAnalyticEvents(
        event = "Signature Request Handled",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to rawRequest.dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to rawRequest.dAppMetaData.url,
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.Key.BLOCKCHAIN to network.name,
            AnalyticsParam.Key.TYPE to securityStatus.toAnalyticVerificationStatus(),
        ),
    )

    class SignatureRequestFailed(
        rawRequest: WcSdkSessionRequest,
        network: Network,
        errorCode: String,
    ) : WcAnalyticEvents(
        event = "Signature Request Failed",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to rawRequest.dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to rawRequest.dAppMetaData.url,
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.Key.BLOCKCHAIN to network.name,
            AnalyticsParam.Key.ERROR_CODE to errorCode,
        ),
    )

    class SignatureRequestReceivedFailed(
        rawRequest: WcSdkSessionRequest,
        blockchain: String,
        errorCode: String,
    ) : WcAnalyticEvents(
        event = "Signature Request Received with Failed",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to rawRequest.dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to rawRequest.dAppMetaData.url,
            AnalyticsParam.Key.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.Key.BLOCKCHAIN to blockchain,
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
        event = "Button - Cancel",
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

    class NoticeSecurityAlert(
        dAppMetaData: WcAppMetaData,
        securityStatus: CheckDAppResult,
        source: Source,
    ) : WcAnalyticEvents(
        event = "Notice - Security Alert",
        params = mapOf(
            AnalyticsParam.Key.DAPP_NAME to dAppMetaData.name,
            AnalyticsParam.Key.DAPP_URL to dAppMetaData.url,
            AnalyticsParam.Key.SOURCE to when (source) {
                Source.Domain -> "Domain"
                Source.SmartContract -> "Smart Contract"
            },
            AnalyticsParam.Key.TYPE to securityStatus.toAnalyticVerificationStatus(),
        ),
    ) {
        enum class Source { Domain, SmartContract }
    }

    enum class DAppVerificationStatus(val status: String) {
        Verified("Verified"),
        Risky("Risky"),
        Unknown("Unknown"),
    }

    fun CheckDAppResult.toAnalyticVerificationStatus(): String = when (this) {
        SAFE -> DAppVerificationStatus.Verified
        UNSAFE -> DAppVerificationStatus.Risky
        FAILED_TO_VERIFY -> DAppVerificationStatus.Unknown
    }.status

    companion object {

        const val NETWORKS = "Networks"
        const val DOMAIN_VERIFICATION = "Domain Verification"
        const val WC_CATEGORY_NAME = "Wallet Connect"
    }
}