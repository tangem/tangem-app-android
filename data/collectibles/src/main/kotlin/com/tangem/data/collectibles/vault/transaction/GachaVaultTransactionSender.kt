package com.tangem.data.collectibles.vault.transaction

import arrow.core.Either
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.delegate.SessionKeyPublicKey
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.wallet.UserWallet

/** Who pays for the vault's transactions: the wallet itself in SOL, or the gasless sponsor. */
internal interface GachaVaultTransactionSender {

    /** For a device that already holds the owner key, so the vault read costs no card session. */
    suspend fun initOrUpdateVault(
        userWallet: UserWallet,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit>

    /** Derives the owner key and signs the set-up in the same card session: one tap. */
    suspend fun deriveOwnerAndSetUpVault(
        userWallet: UserWallet.Cold,
        sessionKey: SessionKeyPublicKey,
        limits: GachaVaultLimits,
    ): Either<CollectiblesAccountError, Unit>

    /**
     * Not idempotent: never call again for the same [memo] while the outcome is unknown.
     *
     * @param signer signs with the session key
     * @return the transaction signature
     */
    @Suppress("LongParameterList")
    suspend fun sendPurchasePack(
        userWallet: UserWallet,
        sessionKey: SessionKeyPublicKey,
        amount: Long,
        feeAmount: Long,
        memo: String,
        signer: suspend (message: ByteArray) -> Result<ByteArray>,
    ): Either<CollectiblesAccountError, String>

    /**
     * Moves USDC out of the vault with `withdraw_token`, signed by the owner. Not idempotent: never call again
     * while the outcome is unknown.
     *
     * @param mint the token to withdraw
     * @param amount base units of the token
     * @return the transaction signature
     */
    suspend fun withdraw(
        userWallet: UserWallet,
        mint: String,
        recipient: String,
        amount: Long,
    ): Either<CollectiblesAccountError, String>
}