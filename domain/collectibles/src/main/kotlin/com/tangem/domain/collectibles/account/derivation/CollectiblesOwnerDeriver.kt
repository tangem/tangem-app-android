package com.tangem.domain.collectibles.account.derivation

import arrow.core.Either
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.models.wallet.UserWalletId

/**
 * The derivation path of the collectibles (gacha) vault owner key.
 *
 * Every node is hardened on purpose: the key is an ed25519 (SLIP-0010) one, and that curve cannot derive a
 * non-hardened node at all, so a path like `m/44'/501'/999700'/0/0` would yield no key and no address.
 *
 * `999700'` continues the app's range of reserved keys (`999999'` Visa, `999998'` money account, `999899'`
 * Polymarket owner, `888888'` joint account owners) rather than naming a chain account.
 *
 * Must match iOS byte-for-byte — the vault address is `PDA(["vault", owner])`, so a different path means a
 * different vault for the same wallet.
 */
const val COLLECTIBLES_OWNER_DERIVATION_PATH: String = "m/44'/501'/999700'/0'/0'"

/**
 * Derives the collectibles vault owner — the ed25519 key at [COLLECTIBLES_OWNER_DERIVATION_PATH], base58-encoded
 * the way Solana addresses are. Persists the derived key, so only the first call needs the card.
 */
interface CollectiblesOwnerDeriver {

    /**
     * Derives the owner key, asking the card for it when it is not stored yet — which opens a card session.
     * Only ever reach this from an explicit user action.
     */
    suspend fun deriveOwner(userWalletId: UserWalletId): Either<CollectiblesAccountError, String>

    /**
     * The owner address if the key is already stored on this device, `null` otherwise. Never opens a card session.
     */
    suspend fun storedOwner(userWalletId: UserWalletId): String?
}