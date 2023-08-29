package com.tangem.domain.wallets.models

sealed interface UpdateWalletError {

    object DataError : UpdateWalletError
}