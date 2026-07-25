package com.tangem.domain.polymarket.derivation

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketDerivationError

/**
 * The Polymarket owner-EOA derivation path (D2 slot), approved 2026-07-21.
 * MUST match iOS/SDK byte-for-byte — pinned by a unit test. See APP-7a spec.
 */
const val OWNER_DERIVATION_PATH: String = "m/44'/60'/999997'/0/0"

/**
 * Derives the Polymarket owner EOA — the secp256k1 key on [OWNER_DERIVATION_PATH] converted to an
 * ERC-55 checksummed address. Persists the derived key. Cold and Hot wallets both supported.
 */
interface PolymarketEoaDeriver {

    suspend fun deriveOwnerEoa(userWalletId: UserWalletId): Either<PolymarketDerivationError, String>
}