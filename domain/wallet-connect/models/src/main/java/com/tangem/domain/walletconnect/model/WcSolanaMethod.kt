package com.tangem.domain.walletconnect.model

sealed interface WcSolanaMethod : WcMethod {

    data class SignMessage(
        val pubKey: String,
        val message: String,
    ) : WcSolanaMethod

    data class SignTransaction(
        val transaction: String,
    ) : WcSolanaMethod

    data class SignAllTransaction(
        val transaction: List<String>,
    ) : WcSolanaMethod
}