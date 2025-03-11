package com.tangem.domain.wallets.models

sealed interface UpdateWalletError {

    data object NameAlreadyExists : UpdateWalletError

    data class DataError(val cause: Throwable) : UpdateWalletError
}