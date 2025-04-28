package com.tangem.domain.walletconnect.model

sealed interface WcEthMethod : WcMethod {

    data class MessageSign(
        val rawMessage: String,
        val account: String,
        val humanMsg: String,
    ) : WcEthMethod

    data class SendTransaction(
        val transaction: WcEthTransactionParams,
    ) : WcEthMethod

    data class SignTransaction(
        val transaction: WcEthTransactionParams,
    ) : WcEthMethod
}