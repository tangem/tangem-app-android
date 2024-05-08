package com.tangem.domain.wallets.models

sealed class GetUserWalletError {

    data object NotFound : GetUserWalletError()
}
