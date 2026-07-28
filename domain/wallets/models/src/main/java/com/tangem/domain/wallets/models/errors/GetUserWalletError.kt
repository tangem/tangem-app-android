package com.tangem.domain.wallets.models.errors

sealed class GetUserWalletError {

    data object UserWalletNotFound : GetUserWalletError()
}