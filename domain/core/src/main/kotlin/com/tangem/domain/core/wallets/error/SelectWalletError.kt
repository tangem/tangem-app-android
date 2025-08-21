package com.tangem.domain.core.wallets.error

sealed interface SelectWalletError {

    data object UnableToSelectUserWallet : SelectWalletError
}