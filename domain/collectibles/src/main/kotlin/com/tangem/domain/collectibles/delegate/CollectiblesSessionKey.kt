package com.tangem.domain.collectibles.delegate

import arrow.core.Either
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.collectibles.CollectiblesAccountError

/**
 * Custodian of the `session_key` of a collectibles account's gacha vault: an ed25519 keypair in the hot SDK
 * keystore, deliberately not a wallet — absent from the wallets list, bound to the vault only by its on-chain
 * `session_key` field, which `init_vault` sets and `update_vault` rotates.
 *
 * Keyed by [AccountId], so a wallet's other accounts cannot reach it. Signing never prompts: a spin is signed
 * while the user watches an animation, so a stolen phone is limited by the vault's caps, not by a local prompt.
 */
interface CollectiblesSessionKey {

    /** Returns the existing session key, creating one on first call. Idempotent. */
    suspend fun provision(accountId: AccountId): Either<CollectiblesAccountError, SessionKeyPublicKey>

    /** Reads the existing session key without ever creating one; null if there is none or it cannot be read. */
    suspend fun getPublicKeyOrNull(accountId: AccountId): SessionKeyPublicKey?

    /** Signs whatever it is handed — deciding that a transaction is safe to sign is the caller's job. */
    suspend fun sign(accountId: AccountId, message: ByteArray): Either<CollectiblesAccountError, ByteArray>

    /** Drops the key and its keystore entry; the vault points at it until `update_vault` rotates it. */
    suspend fun forget(accountId: AccountId)
}