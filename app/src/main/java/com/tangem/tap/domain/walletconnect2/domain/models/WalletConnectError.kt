package com.tangem.tap.domain.walletconnect2.domain.models

sealed class WalletConnectError(val error: String) : Exception() {

    data object UnsupportedDApp : WalletConnectError("UnsupportedDApp")

    data class ApprovalErrorMissingNetworks(
        val missingChains: List<String>,
    ) : WalletConnectError("ApprovalErrorMissingNetworks")

    data class ApprovalErrorAddNetwork(
        val networks: List<String>,
    ) : WalletConnectError("ApprovalErrorAddNetwork")

    data class ApprovalErrorUnsupportedNetwork(
        val unsupportedNetworks: List<String>,
    ) : WalletConnectError("ApprovalErrorUnsupportedNetwork")

    data class ExternalApprovalError(
        override val message: String?,
    ) : WalletConnectError("ExternalApprovalError")

    data class UnknownError(
        override val message: String,
    ) : WalletConnectError(message)

    data object WrongUserWallet : WalletConnectError("WrongUserWallet")
    data object UnsupportedMethod : WalletConnectError("UnsupportedMethod")
    data object SigningError : WalletConnectError("SigningError")
    data object ValidationError : WalletConnectError("ValidationError")
}