package com.tangem.domain.walletconnect

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.CheckDAppResult.*
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.WcAnalyticEvents.DAppVerificationStatus
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.utils.extensions.mapNotNullValues

sealed class WcAnalyticEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = WC_CATEGORY_NAME, event = event, params = params) {

    class ScreenOpened : WcAnalyticEvents(event = "WC Screen Opened"), AppsFlyerIncludedEvent

    class NewPairInitiated(source: WcPairRequest.Source) : WcAnalyticEvents(
        event = "Session Initiated",
        params = mapOf(
            AnalyticsParam.SOURCE to when (source) {
                WcPairRequest.Source.QR -> "QR"
                WcPairRequest.Source.DEEPLINK -> "DeepLink"
                WcPairRequest.Source.CLIPBOARD -> "Clipboard"
                WcPairRequest.Source.ETC -> "etc"
            },
        ),
    )

    class PairButtonConnect(
        dAppName: String,
        accountDerivation: Int?,
    ) : WcAnalyticEvents(
        event = "Button - Connect",
        params = buildMap {
            put(AnalyticsParam.DAPP_NAME, dAppName)
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    )

    class PairRequested(
        dAppName: String,
        dAppUrl: String,
        network: Set<Network>,
        domainVerification: CheckDAppResult,
    ) : WcAnalyticEvents(
        event = "dApp Connection Requested",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName,
            AnalyticsParam.DAPP_URL to dAppUrl,
            NETWORKS to network.joinToString(",") { it.name },
            DOMAIN_VERIFICATION to domainVerification.toAnalyticVerificationStatus(),
        ),
    )

    class PairFailed(
        errorCode: String,
        errorMessage: String,
    ) : WcAnalyticEvents(
        event = "Session Failed",
        params = mapOf(
            AnalyticsParam.ERROR_CODE to errorCode,
            AnalyticsParam.ERROR_DESCRIPTION to errorMessage,
        ),
    )

    class DAppConnected(
        sessionProposal: WcSessionProposal,
        sessionForApprove: WcSessionApprove,
        securityStatus: CheckDAppResult,
    ) : WcAnalyticEvents(
        event = "dApp Connected",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to sessionProposal.dAppMetaData.name,
            AnalyticsParam.DAPP_URL to sessionProposal.dAppMetaData.url,
            AnalyticsParam.BLOCKCHAIN to sessionForApprove.network.joinToString(",") { it.name },
            DOMAIN_VERIFICATION to securityStatus.toAnalyticVerificationStatus(),
        ),
    ), AppsFlyerIncludedEvent

    class DAppConnectionFailed(
        errorCode: String,
        errorMessage: String,
    ) : WcAnalyticEvents(
        event = "dApp Connection Failed",
        params = mapOf(
            AnalyticsParam.ERROR_CODE to errorCode,
            AnalyticsParam.ERROR_DESCRIPTION to errorMessage,
        ),
    )

    class SessionDisconnected(dAppMetaData: WcAppMetaData) : WcAnalyticEvents(
        event = "dApp Disconnected",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppMetaData.name,
            AnalyticsParam.DAPP_URL to dAppMetaData.url,
        ),
    )

    class TransactionDetailsOpened(
        rawRequest: WcSdkSessionRequest,
        network: Network,
    ) : WcAnalyticEvents(
        event = "Transaction Details Opened",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to rawRequest.dAppMetaData.name,
            AnalyticsParam.DAPP_URL to rawRequest.dAppMetaData.url,
            AnalyticsParam.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.BLOCKCHAIN to network.name,
        ),
    )

    class SignatureRequestReceived(
        rawRequest: WcSdkSessionRequest,
        network: Network,
        emulationStatus: EmulationStatus?,
        accountDerivation: Int?,
        securityStatus: CheckDAppResult,
    ) : WcAnalyticEvents(
        event = "Signature Request Received",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to rawRequest.dAppMetaData.name,
            AnalyticsParam.DAPP_URL to rawRequest.dAppMetaData.url,
            AnalyticsParam.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.BLOCKCHAIN to network.name,
            AnalyticsParam.EMULATION_STATUS to emulationStatus?.status,
            AnalyticsParam.TYPE to securityStatus.toAnalyticVerificationStatus(),
            AnalyticsParam.ACCOUNT_DERIVATION to accountDerivation?.toString(),
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
        accountDerivation: Int?,
    ) : WcAnalyticEvents(
        event = "Signature Request Handled",
        params = buildMap {
            put(AnalyticsParam.DAPP_NAME, rawRequest.dAppMetaData.name)
            put(AnalyticsParam.DAPP_URL, rawRequest.dAppMetaData.url)
            put(AnalyticsParam.METHOD_NAME, rawRequest.request.method)
            put(AnalyticsParam.BLOCKCHAIN, network.name)
            put(AnalyticsParam.TYPE, securityStatus.toAnalyticVerificationStatus())
            accountDerivation?.let {
                put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString())
            }
        },
    ), AppsFlyerIncludedEvent

    class SignatureRequestFailed(
        rawRequest: WcSdkSessionRequest,
        network: Network,
        errorCode: String,
        errorMessage: String,
        accountDerivation: Int?,
    ) : WcAnalyticEvents(
        event = "Signature Request Failed",
        params = buildMap {
            put(AnalyticsParam.DAPP_NAME, rawRequest.dAppMetaData.name)
            put(AnalyticsParam.DAPP_URL, rawRequest.dAppMetaData.url)
            put(AnalyticsParam.METHOD_NAME, rawRequest.request.method)
            put(AnalyticsParam.BLOCKCHAIN, network.name)
            put(AnalyticsParam.ERROR_CODE, errorCode)
            put(AnalyticsParam.ERROR_DESCRIPTION, errorMessage)
            accountDerivation?.let { put(AnalyticsParam.ACCOUNT_DERIVATION, it.toString()) }
        },
    )

    class SignatureRequestReceivedFailed(
        rawRequest: WcSdkSessionRequest,
        blockchain: String,
        errorCode: String,
        errorMessage: String,
    ) : WcAnalyticEvents(
        event = "Signature Request Received with Failed",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to rawRequest.dAppMetaData.name,
            AnalyticsParam.DAPP_URL to rawRequest.dAppMetaData.url,
            AnalyticsParam.METHOD_NAME to rawRequest.request.method,
            AnalyticsParam.BLOCKCHAIN to blockchain,
            AnalyticsParam.ERROR_CODE to errorCode,
            AnalyticsParam.ERROR_DESCRIPTION to errorMessage,
        ),
    )

    class ButtonSign(
        rawRequest: WcSdkSessionRequest,
    ) : WcAnalyticEvents(
        event = "Button - Sign",
        params = mapOf(
            AnalyticsParam.METHOD_NAME to rawRequest.request.method,
        ),
    )

    class ButtonCancel(
        type: Type,
    ) : WcAnalyticEvents(
        event = "Button - Cancel",
        params = mapOf(
            AnalyticsParam.TYPE to type.type,
        ),
    ) {
        enum class Type(val type: String) {
            Connection("Connection"),
            Sign("Sign"),
        }
    }

    class ButtonDisconnectAll : WcAnalyticEvents(
        event = "Button - Disconnect All",
    )

    class ButtonDisconnect(
        session: WcSession,
    ) : WcAnalyticEvents(
        event = "Button - Disconnect",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to session.sdkModel.appMetaData.name,
            AnalyticsParam.DAPP_URL to session.sdkModel.appMetaData.url,
        ),
    )

    class NoticeSecurityAlert(
        dAppMetaData: WcAppMetaData,
        securityStatus: CheckDAppResult,
        source: Source,
    ) : WcAnalyticEvents(
        event = "Notice - Security Alert",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppMetaData.name,
            AnalyticsParam.DAPP_URL to dAppMetaData.url,
            AnalyticsParam.SOURCE to when (source) {
                Source.Domain -> "Domain"
                Source.SmartContract -> "Smart Contract"
            },
            AnalyticsParam.TYPE to securityStatus.toAnalyticVerificationStatus(),
        ),
    ) {
        enum class Source { Domain, SmartContract }
    }

    data class SolanaLargeTransaction(
        val dappName: String,
    ) : WcAnalyticEvents(
        event = "Solana Large Transaction",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dappName,
        ),
    )

    data class SolanaLargeTransactionStatus(
        val status: Status,
    ) : WcAnalyticEvents(
        event = "Solana Large Transaction Status",
        params = mapOf(
            AnalyticsParam.STATUS to status.value,
        ),
    ) {
        enum class Status(val value: String) {
            Success("Success"),
            Failed("Failed"),
        }
    }

    enum class DAppVerificationStatus(val status: String) {
        Verified("Verified"),
        Risky("Risky"),
        Unknown("Unknown"),
    }

    companion object {

        const val NETWORKS = "Networks"
        const val DOMAIN_VERIFICATION = "Domain Verification"
        const val WC_CATEGORY_NAME = "Wallet Connect"
    }
}

fun CheckDAppResult.toAnalyticVerificationStatus(): String = when (this) {
    SAFE -> DAppVerificationStatus.Verified
    UNSAFE -> DAppVerificationStatus.Risky
    FAILED_TO_VERIFY -> DAppVerificationStatus.Unknown
}.status