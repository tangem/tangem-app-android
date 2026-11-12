package com.tangem.data.collectibles.vault.transaction

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.WalletManager
import com.tangem.data.collectibles.vault.toEither
import com.tangem.domain.collectibles.delegate.SessionKeyPublicKey
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.logging.TangemLogger

/** Built from the wallet's stored derivations, so the owner key has to be persisted before this is called. */
@Suppress("DEPRECATION")
internal suspend fun WalletManagersFacade.vaultWalletManager(
    userWalletId: UserWalletId,
    network: Network,
): Either<BlockchainError, WalletManager> {
    val walletManager = getOrCreateWalletManager(userWalletId = userWalletId, network = network)
        ?: return BlockchainSdkError.CustomError(
            "Failed to create or retrieve wallet manager: userWalletId=$userWalletId, network=${network.id}",
        ).left()

    return walletManager.right()
}

/**
 * The transaction that brings the vault to the state [sessionKey] needs, or null when it is there already.
 *
 * Reading first keeps it at one signature: `init_vault` over an existing vault fails with `already in use`.
 */
internal suspend fun WalletManager.buildInitOrUpdateVault(
    sessionKey: SessionKeyPublicKey,
    limits: GachaVaultLimits,
): Either<BlockchainError, TransactionData.Compiled?> = either {
    val existing = getCollectiblesVault().toEither()
        .onLeft { TangemLogger.d("vault: getCollectiblesVault failed: ${it.customMessage}") }
        .bind()

    when {
        existing == null -> {
            TangemLogger.d(
                "vault: does the User Account PDA exist on chain? -> " +
                    "No, creating the User Account PDA (init_vault) (tap)",
            )
            buildInitCollectiblesVault(
                sessionKey = sessionKey.base58,
                perSpinCap = limits.perSpinCap,
                dailyCap = limits.dailyCap,
            ).toEither().bind()
        }
        existing.sessionKey != sessionKey.base58 -> {
            TangemLogger.d(
                "vault: is the hot key the same as in the User Account PDA? -> " +
                    "No, updating the key in the PDA (update_vault) (tap)",
            )
            buildUpdateCollectiblesVault(
                sessionKey = sessionKey.base58,
                perSpinCap = null,
                dailyCap = null,
            ).toEither().bind()
        }
        else -> {
            TangemLogger.d(
                "vault: is the hot key the same as in the User Account PDA? -> Yes, no transaction needed",
            )
            null
        }
    }
}