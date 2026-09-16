package com.tangem.data.collectibles.vault

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Network the gacha program is addressed through — built on [COLLECTIBLES_OWNER_DERIVATION_PATH], not the wallet's
 * ordinary Solana account: the vault is `PDA(["vault", owner])` and the wallet manager's address on this network is
 * what the SDK takes as `cold_owner`.
 */
internal class CollectiblesOwnerNetworkProvider @Inject constructor(
    private val networkFactory: NetworkFactory,
    private val ownerDeriver: CollectiblesOwnerDeriver,
) {

    /**
     * [GachaVaultError.WalletUnavailable] while the owner key is not on the device: the vault cannot be addressed
     * without it, and callers route that to onboarding rather than to a failure.
     */
    suspend fun create(userWallet: UserWallet): Either<GachaVaultError, Network> {
        if (ownerDeriver.storedOwner(userWallet.walletId) == null) {
            TangemLogger.d(messageString = "Collectibles vault: the owner key is not derived yet")
            return GachaVaultError.WalletUnavailable.left()
        }

        val blockchain = userWallet.collectiblesVaultBlockchain
        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = COLLECTIBLES_OWNER_DERIVATION_PATH,
            userWallet = userWallet,
        ) ?: run {
            TangemLogger.d(messageString = "Collectibles vault: no network for $blockchain")
            return GachaVaultError.WalletUnavailable.left()
        }

        return network.right()
    }
}

/**
 * The chain the gacha vault is addressed on. A hot wallet has no card behind it, so it is never a test one.
 */
private val UserWallet.collectiblesVaultBlockchain: Blockchain
    get() = if (this is UserWallet.Cold && cardTypesResolver.isTestCard()) {
        Blockchain.SolanaTestnet
    } else {
        Blockchain.Solana
    }