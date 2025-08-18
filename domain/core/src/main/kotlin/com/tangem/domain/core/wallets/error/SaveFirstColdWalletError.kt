package com.tangem.domain.core.wallets.error

sealed interface SaveFirstColdWalletError {
    data object CreateWalletError : SaveFirstColdWalletError
    data class SaveError(val error: SaveWalletError) : SaveFirstColdWalletError
    data class SelectError(val error: SelectWalletError) : SaveFirstColdWalletError
}