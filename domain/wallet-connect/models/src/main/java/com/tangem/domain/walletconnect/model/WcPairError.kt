package com.tangem.domain.walletconnect.model

sealed class WcPairError(override val message: String) : Exception(message) {

    data object UnsupportedDApp : WcPairError("UnsupportedDApp")
    data class UnsupportedNetworks(
        val chains: Set<String>,
    ) : WcPairError("ApprovalErrorMissingNetworks")

    data class ExternalApprovalError(
        override val message: String,
    ) : WcPairError("ExternalApprovalError")

    data class Unknown(override val message: String) : WcPairError(message)
}