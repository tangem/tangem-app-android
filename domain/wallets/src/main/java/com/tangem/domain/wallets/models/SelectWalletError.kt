package com.tangem.domain.wallets.models

sealed interface SelectWalletError {

    object DataError : SelectWalletError

    object UnableToSelectUserWallet : SelectWalletError
}
