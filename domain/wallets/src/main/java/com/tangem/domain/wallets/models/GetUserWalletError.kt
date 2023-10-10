package com.tangem.domain.wallets.models

sealed class GetUserWalletError {

    data class DataError(val cause: Throwable) : GetUserWalletError()

    object UserWalletNotFound : GetUserWalletError()
}