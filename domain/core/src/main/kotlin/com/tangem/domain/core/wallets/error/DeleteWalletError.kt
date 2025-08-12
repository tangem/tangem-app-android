package com.tangem.domain.core.wallets.error

sealed interface DeleteWalletError {

    data object UnableToDelete : DeleteWalletError
}