package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.canHandleBlockchain
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.supportedTokens
import com.tangem.domain.common.util.cardTypesResolver
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