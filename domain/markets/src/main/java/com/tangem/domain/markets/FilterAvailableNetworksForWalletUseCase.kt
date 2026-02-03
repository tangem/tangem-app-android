package com.tangem.domain.markets

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.card.common.extensions.supportedBlockchains
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.requireUserWalletsSync
import com.tangem.domain.models.wallet.UserWalletId

class FilterAvailableNetworksForWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val excludedBlockchains: ExcludedBlockchains,
) {

    /**
     * Filters [networks] list according supported blockchains for this card
     * If [userWalletId] not found then returns the same list
     */
    operator fun invoke(
        userWalletId: UserWalletId,
        networks: Set<TokenMarketInfo.Network>,
    ): Set<TokenMarketInfo.Network> {
        val userWallet = userWalletsListRepository.requireUserWalletsSync()
            .firstOrNull { it.walletId == userWalletId }
            ?: return networks.toSet()

        val supportedBlockchains = userWallet.supportedBlockchains(
            excludedBlockchains = excludedBlockchains,
        )

        return networks.filter { network ->
            val blockchain = Blockchain.fromNetworkId(network.networkId)
            supportedBlockchains.contains(blockchain)
        }.toSet()
    }
}