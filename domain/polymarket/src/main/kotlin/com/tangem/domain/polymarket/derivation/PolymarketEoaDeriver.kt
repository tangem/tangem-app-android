package com.tangem.domain.polymarket.derivation

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketDerivationError

/**
 * The Polymarket owner-EOA derivation path. Must match iOS/SDK byte-for-byte — pinned by a unit test.
 */
const val POLYMARKET_OWNER_DERIVATION_PATH: String = "m/44'/60'/999997'/0/0"

/**
 * Derives the Polymarket owner EOA — the secp256k1 key on [POLYMARKET_OWNER_DERIVATION_PATH] converted to an
 * ERC-55 checksummed address. Persists the derived key. Cold and Hot wallets both supported.
 */
interface PolymarketEoaDeriver {

    /**
     * Derives the owner EOA, deriving the key on the wallet when it is not stored yet — which opens a card
     * session on Cold and unlocks the seed on Hot. Only ever reach this from an explicit user action.
     */
    suspend fun deriveOwnerEoa(userWalletId: UserWalletId): Either<PolymarketDerivationError, String>

    /**
     * The owner EOA if the key is already stored on this device, `null` otherwise.
     *
     * Never opens a card session and never unlocks the wallet, so it is safe to call from merely opening a
     * screen. `null` means "not derived here yet" — never "this wallet has no Polymarket account": a wallet
     * onboarded on another device answers `null` too, and only [deriveOwnerEoa] can tell the two apart.
     */
    suspend fun storedOwnerEoa(userWalletId: UserWalletId): String?
}