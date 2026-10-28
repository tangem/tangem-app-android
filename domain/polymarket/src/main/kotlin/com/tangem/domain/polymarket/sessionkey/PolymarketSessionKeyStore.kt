package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError

interface PolymarketSessionKeyStore {

    /** @return the lower-case `0x`-prefixed address, `null` when no key is held or it is unreadable */
    suspend fun heldAddress(userWalletId: UserWalletId): String?

    /**
     * @return the address of the held key, a new key's when none is stored, or `Failure` when the stored one cannot
     * be read; an unreadable key is never replaced
     */
    suspend fun getOrCreate(userWalletId: UserWalletId): Either<PolymarketSessionKeyError, String>

    /**
     * @param address the key expected to sign; `KeyMissing` when the held key is another one
     * @param digest the 32-byte hash to sign
     * @return the 65-byte `r‖s‖v` signature, `v` being 27 or 28
     */
    suspend fun sign(
        userWalletId: UserWalletId,
        address: String,
        digest: ByteArray,
    ): Either<PolymarketSessionKeyError, ByteArray>

    /** @return `Failure` only when the mapping cannot be removed; a hot wallet left behind is logged */
    suspend fun delete(userWalletId: UserWalletId): Either<PolymarketSessionKeyError, Unit>
}