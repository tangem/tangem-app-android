package com.tangem.domain.wallets.models

sealed interface CreateHotWalletError {

    data object LimitReached : CreateHotWalletError

    data class Failed(val cause: Throwable? = null) : CreateHotWalletError
}