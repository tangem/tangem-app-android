package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.card.EllipticCurve
import com.tangem.data.common.currency.getNetwork
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.extensions.*
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.NetworksCompatibilityRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultNetworksCompatibilityRepository(
    private val userWalletsStore: UserWalletsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : NetworksCompatibilityRepository {

    /**
     * @return returns true if either the network is not Solana (check is not relevant) or if it is Solana and
     * UserWallet supports tokens on Solana Network
     */
    @Throws(IllegalArgumentException::class)
    override suspend fun areSolanaTokensSupportedIfRelevant(networkId: String, userWalletId: UserWalletId): Boolean {
        return withContext(dispatchers.io) {
            val scanResponse = getWalletOrThrow(userWalletId).scanResponse
            val blockchain = getBlockchainOrThrow(networkId)
            val blockchainsSupportingTokens = scanResponse.card.supportedTokens(scanResponse.cardTypesResolver)
            blockchain != Blockchain.Solana || blockchainsSupportingTokens.contains(Blockchain.Solana)
        }
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun areTokensSupportedByNetwork(networkId: String, userWalletId: UserWalletId): Boolean {
        return withContext(dispatchers.io) {
            val scanResponse = getWalletOrThrow(userWalletId).scanResponse
            val blockchain = getBlockchainOrThrow(networkId)
            val blockchainsSupportingTokens = scanResponse.card.supportedTokens(scanResponse.cardTypesResolver)
            scanResponse.card.canHandleToken(
                supportedTokens = blockchainsSupportingTokens,
                blockchain = blockchain,
                cardTypesResolver = scanResponse.cardTypesResolver,
            )
        }
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun isNetworkSupported(networkId: String, userWalletId: UserWalletId): Boolean {
        return withContext(dispatchers.io) {
            val scanResponse = getWalletOrThrow(userWalletId).scanResponse
            val blockchain = getBlockchainOrThrow(networkId)
            scanResponse.card.canHandleBlockchain(
                blockchain = blockchain,
                cardTypesResolver = scanResponse.cardTypesResolver,
            )
        }
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun getSupportedNetworks(userWalletId: UserWalletId): List<Network> {
        val scanResponse = getWalletOrThrow(userWalletId).scanResponse
        return Blockchain.entries
            .filter { blockchain ->
                scanResponse.card.supportedBlockchains(scanResponse.cardTypesResolver).contains(blockchain)
            }
            .sortedBy(Blockchain::fullName)
            .mapNotNull { blockchain ->
                getNetwork(blockchain, null, scanResponse.derivationStyleProvider)
            }
    }

    override suspend fun requiresHardenedDerivationOnly(networkId: String, userWalletId: UserWalletId): Boolean {
        val scanResponse = getWalletOrThrow(userWalletId).scanResponse
        val config = CardConfig.createConfig(scanResponse.card)
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return false

        return config.primaryCurve(blockchain) == EllipticCurve.Ed25519Slip0010 &&
            scanResponse.cardTypesResolver.isWallet2()
    }

    override fun areTokensSupportedByNetwork(networkId: String): Boolean {
        return Blockchain.fromNetworkId(networkId)?.canHandleTokens() ?: false
    }

    private suspend fun getWalletOrThrow(userWalletId: UserWalletId): UserWallet {
        return requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Requested UserWallet not found"
        }
    }

    private fun getBlockchainOrThrow(networkId: String): Blockchain {
        return requireNotNull(Blockchain.fromNetworkId(networkId)) {
            "Requested network not found"
        }
    }
}
