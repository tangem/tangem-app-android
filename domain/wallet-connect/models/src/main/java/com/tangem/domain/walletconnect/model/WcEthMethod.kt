package com.tangem.domain.walletconnect.model

sealed interface WcEthMethod : WcMethod {

    data class MessageSign(
        val message: String,
        val account: String,
    ) : WcEthMethod

    data class SendTransaction(
        val transaction: WcEthTransactionParams,
    ) : WcEthMethod

    data class SignTransaction(
        val transaction: WcEthTransactionParams,
    ) : WcEthMethod
}