package com.tangem.domain.wallets.models.errors

sealed class WalletCardsBackupError {

    data object NoInternetConnection : WalletCardsBackupError()

    data class Unexpected(val cause: Throwable?) : WalletCardsBackupError()
}