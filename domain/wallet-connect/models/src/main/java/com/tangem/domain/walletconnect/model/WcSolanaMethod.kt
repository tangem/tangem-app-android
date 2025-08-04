package com.tangem.domain.walletconnect.model

sealed interface WcSolanaMethod : WcMethod {

    data class SignMessage(
        val pubKey: String,
        val rawMessage: String,
        val humanMsg: String,
    ) : WcSolanaMethod

    data class SignTransaction(
        val transaction: String,
        val address: String?,
    ) : WcSolanaMethod

    data class SignAllTransaction(
        val transaction: List<String>,
    ) : WcSolanaMethod
}