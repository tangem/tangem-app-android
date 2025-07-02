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
    data class Pair(val request: WcPairRequest) : WcInnerRoute

    @Serializable
    data class UnsupportedMethodAlert(override val rawRequest: WcSdkSessionRequest) : Method
}