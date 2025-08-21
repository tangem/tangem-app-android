package com.tangem.domain.markets

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.card.common.extensions.supportedBlockchains
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.core.wallets.requireUserWalletsSync

class FilterAvailableNetworksForWalletUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
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
        val userWallet = getWallets().firstOrNull {
            it.walletId == userWalletId
        } ?: return networks.toSet()

        val supportedBlockchains = userWallet.supportedBlockchains(
            excludedBlockchains = excludedBlockchains,
        )

        return networks.filter {
            val blockchain = Blockchain.fromNetworkId(it.networkId)
            supportedBlockchains.contains(blockchain)
        }.toSet()
    }

    private fun getWallets() = if (useNewRepository) {
        userWalletsListRepository.requireUserWalletsSync()
    } else {
        userWalletsListManager.userWalletsSync
    }
}