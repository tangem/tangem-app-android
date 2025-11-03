package com.tangem.domain.common.wallets.error

sealed interface SelectWalletError {

    data object UnableToSelectUserWallet : SelectWalletError
}