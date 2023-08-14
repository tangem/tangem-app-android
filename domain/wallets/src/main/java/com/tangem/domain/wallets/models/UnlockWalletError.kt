package com.tangem.domain.wallets.models

sealed interface UnlockWalletError {

    object CommonError : UnlockWalletError
}