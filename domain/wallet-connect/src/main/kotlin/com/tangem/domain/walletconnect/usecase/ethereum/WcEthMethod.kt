package com.tangem.domain.walletconnect.usecase.ethereum

import com.tangem.domain.walletconnect.model.WcMethod

sealed interface WcEthMethod : WcMethod {

    data class SignMessage(
        val raw: List<String>,
    ) : WcEthMethod
}