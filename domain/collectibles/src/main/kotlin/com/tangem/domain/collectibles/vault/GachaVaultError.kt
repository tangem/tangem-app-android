@file:Suppress("MagicNumber")

package com.tangem.domain.collectibles.vault

import com.tangem.core.error.UniversalError

sealed class GachaVaultError(override val errorCode: Int) : UniversalError {

    data object NetworkUnsupported : GachaVaultError(errorCode = 109001001)

    data object WalletUnavailable : GachaVaultError(errorCode = 109001002)

    data class Unexpected(val cause: Throwable) : GachaVaultError(errorCode = 109001003)

    /** The transaction is larger than a card can sign, so it is refused before the tap. */
    data object TransactionTooLarge : GachaVaultError(errorCode = 109001004)

    /** The gasless sponsor cannot pay right now: the service is off, refuses the key or is down. */
    data object SponsorUnavailable : GachaVaultError(errorCode = 109001005)

    data object SponsorRateLimited : GachaVaultError(errorCode = 109001006)

    /** The sponsor refused the transaction as built. */
    data class SponsorRejected(val reason: String?) : GachaVaultError(errorCode = 109001007)

    /** The blockhash expired before the sponsor broadcast it, so it has to be signed again. */
    data object TransactionExpired : GachaVaultError(errorCode = 109001008)
}