package com.tangem.domain.walletconnect.model

sealed class WcPairError(
    val code: String,
    override val message: String = "",
) : Exception() {

    data class UriAlreadyUsed(override val message: String) : WcPairError("107 001 001")
    data class PairingFailed(override val message: String) : WcPairError("107 001 002")
    data object InvalidDomainURL : WcPairError("107 001 003")
    data object UnsupportedDomain : WcPairError("107 001 004")
    data class UnsupportedBlockchains(val chains: Set<String>) : WcPairError("107 001 005")

    data object InvalidConnectionRequest : WcPairError("107 002 001")
    data object ProposalExpired : WcPairError("107 002 002")
    data class ApprovalFailed(override val message: String) : WcPairError("107 002 003")
    data object RejectionFailed : WcPairError("107 002 004")
    data class Unknown(override val message: String) : WcPairError(message)
}