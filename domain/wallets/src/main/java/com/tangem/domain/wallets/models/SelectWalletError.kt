package com.tangem.domain.wallets.models

sealed interface SelectWalletError {

    object UnableToSelectUserWallet : SelectWalletError
}