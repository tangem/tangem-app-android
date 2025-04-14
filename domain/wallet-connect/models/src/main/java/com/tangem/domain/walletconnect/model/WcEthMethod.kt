package com.tangem.domain.walletconnect.model

sealed interface WcEthMethod : WcMethod {

    data class PersonalEthSign(
        val message: String,
        val account: String,
    ) : WcEthMethod
}