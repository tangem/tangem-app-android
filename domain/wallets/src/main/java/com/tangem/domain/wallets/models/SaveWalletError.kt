package com.tangem.domain.wallets.models

/**
 * @author Andrew Khokhlov on 14/07/2023
 */
sealed interface SaveWalletError {

    val messageId: Int?

    data class DataError(override val messageId: Int?) : SaveWalletError

    data class WalletAlreadySaved(override val messageId: Int) : SaveWalletError
}
