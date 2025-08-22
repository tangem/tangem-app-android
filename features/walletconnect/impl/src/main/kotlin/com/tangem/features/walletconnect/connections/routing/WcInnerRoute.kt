package com.tangem.features.walletconnect.connections.routing

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface WcInnerRoute : Route {

    @Serializable
    sealed interface Method : WcInnerRoute {
        val rawRequest: WcSdkSessionRequest
    }

    @Serializable
    data class Send(override val rawRequest: WcSdkSessionRequest) : Method

    @Serializable
    data class SignMessage(override val rawRequest: WcSdkSessionRequest) : Method

    @Serializable
    data class AddNetwork(override val rawRequest: WcSdkSessionRequest) : Method

    @Serializable
    data class SwitchNetwork(override val rawRequest: WcSdkSessionRequest) : Method

    @Serializable
    data class Pair(val request: WcPairRequest) : WcInnerRoute

    @Serializable
    data object UnsupportedMethodAlert : WcInnerRoute

    @Serializable
    data object WcDappDisconnected : WcInnerRoute

    @Serializable
    data class TangemUnsupportedNetwork(val networkName: String) : WcInnerRoute

    @Serializable
    data class RequiredAddNetwork(val networkName: String) : WcInnerRoute

    @Serializable
    data class RequiredReconnectWithNetwork(val networkName: String) : WcInnerRoute
}