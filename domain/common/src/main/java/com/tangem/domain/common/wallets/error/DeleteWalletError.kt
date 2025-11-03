package com.tangem.domain.common.wallets.error

sealed interface DeleteWalletError {

    data object UnableToDelete : DeleteWalletError
}