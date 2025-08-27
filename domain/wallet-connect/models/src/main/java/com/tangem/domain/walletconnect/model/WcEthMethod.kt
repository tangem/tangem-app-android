package com.tangem.domain.walletconnect.model

sealed interface WcEthMethod : WcMethod {

    data class MessageSign(
        val rawMessage: String,
        val account: String,
        val humanMsg: String,
    ) : WcEthMethod

    data class SignTypedData(
        val params: WcEthSignTypedDataParams,
        val account: String,
        val dataForSign: String,
    ) : WcEthMethod {
        val humanMsg: String = params.message.orEmpty()
    }

    data class SendTransaction(
        val transaction: WcEthTransactionParams,
    ) : WcEthMethod

    data class SignTransaction(
        val transaction: WcEthTransactionParams,
    ) : WcEthMethod

    data class AddEthereumChain(
        val rawChain: WcEthAddChain,
    ) : WcEthMethod

    data class SwitchEthereumChain(
        val rawChain: WcEthAddChain,
    ) : WcEthMethod
}