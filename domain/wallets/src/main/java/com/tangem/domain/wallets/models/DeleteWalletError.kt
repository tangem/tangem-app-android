package com.tangem.domain.wallets.models

sealed interface DeleteWalletError {

    object DataError : DeleteWalletError
}