package com.tangem.domain.walletconnect.model

sealed class WcPairError(
    val code: String,
    override val message: String = "",
) : Exception() {

    data class UriAlreadyUsed(override val message: String) : WcPairError(code = "107 001 001")
    data class PairingFailed(override val message: String) : WcPairError(code = "107 001 002")
    class InvalidDomainURL : WcPairError(code = "107 001 003")

    data class UnsupportedDApp(val appName: String) : WcPairError(code = "107 001 004")
    data class UnsupportedBlockchains(val chains: Set<String>, val appName: String) : WcPairError(code = "107 001 005")

    data class ApprovalFailed(override val message: String) : WcPairError(code = "107 002 003")
    data class Unknown(override val message: String) : WcPairError(code = "107 003 001")
    data class TimeoutException(override val message: String) : WcPairError(code = "107 003 002")
}