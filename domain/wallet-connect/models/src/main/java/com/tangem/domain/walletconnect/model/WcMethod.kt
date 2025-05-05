package com.tangem.domain.walletconnect.model

sealed interface WcMethod {
    data object Unsupported : WcMethod
}