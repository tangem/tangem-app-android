package com.tangem.domain.wallets.models

/**
[REDACTED_AUTHOR]
 */
sealed interface SaveWalletError {

    object DataError : SaveWalletError

    data class WalletAlreadySaved(val messageId: Int) : SaveWalletError
}