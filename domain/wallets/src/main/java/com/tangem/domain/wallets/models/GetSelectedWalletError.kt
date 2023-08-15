package com.tangem.domain.wallets.models

sealed interface GetSelectedWalletError {

    object DataError : GetSelectedWalletError

    object NoUserWalletSelected : GetSelectedWalletError
}