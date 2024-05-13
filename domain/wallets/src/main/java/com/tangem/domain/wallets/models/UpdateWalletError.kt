package com.tangem.domain.wallets.models

sealed interface UpdateWalletError {

    data object DataError : UpdateWalletError

    data object NameAlreadyExists : UpdateWalletError
}
