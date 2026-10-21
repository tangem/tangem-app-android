package com.tangem.domain.wallets.signing

sealed interface DeriveAndSignError {

    data object UserCancelled : DeriveAndSignError

    data object WalletNotFound : DeriveAndSignError

    data object CardError : DeriveAndSignError

    data object Unknown : DeriveAndSignError
}