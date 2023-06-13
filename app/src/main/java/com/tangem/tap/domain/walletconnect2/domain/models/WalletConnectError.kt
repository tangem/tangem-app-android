package com.tangem.tap.domain.walletconnect2.domain.models

sealed class WalletConnectError : Exception() {
    data class ApprovalErrorMissingNetworks(val missingChains: List<String>) : WalletConnectError()
    data class ApprovalErrorAddNetwork(val networks: List<String>) : WalletConnectError()
    object ApprovalErrorUnsupportedNetwork : WalletConnectError()
    data class ExternalApprovalError(override val message: String?) : WalletConnectError()
    object WrongUserWallet : WalletConnectError()
    object UnsupportedMethod : WalletConnectError()
}