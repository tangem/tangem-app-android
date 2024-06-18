package com.tangem.domain.wallets.models

/**
[REDACTED_AUTHOR]
 */
sealed interface SaveWalletError {

    val messageId: Int?

    data class DataError(override val messageId: Int?) : SaveWalletError

    data class WalletAlreadySaved(override val messageId: Int) : SaveWalletError
}