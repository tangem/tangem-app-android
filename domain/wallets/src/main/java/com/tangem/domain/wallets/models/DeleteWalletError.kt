package com.tangem.domain.wallets.models

sealed interface DeleteWalletError {

    data object UnableToDelete : DeleteWalletError
}