package com.tangem.domain.walletconnect.model

import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest

sealed interface WcMethod {
    data class Unsupported(val request: WcSdkSessionRequest) : WcMethod
}