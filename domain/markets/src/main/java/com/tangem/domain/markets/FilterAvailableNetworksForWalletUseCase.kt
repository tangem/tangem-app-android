package com.tangem.domain.markets

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.card.common.extensions.supportedBlockchains
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.legacy.UserWalletsListManager

class FilterAvailableNetworksForWalletUseCase(
    private val userWalletsListManager: UserWalletsListManager,
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
        val userWallet = userWalletsListManager.userWalletsSync.firstOrNull {
            it.walletId == userWalletId
        } ?: return networks.toSet()

        return when (userWallet) {
            is UserWallet.Cold -> {
                val supportedBlockchains = userWallet.scanResponse.card.supportedBlockchains(
                    cardTypesResolver = userWallet.scanResponse.cardTypesResolver,
                    excludedBlockchains = excludedBlockchains,
                )

                networks.filter {
                    val blockchain = Blockchain.fromNetworkId(it.networkId)
                    supportedBlockchains.contains(blockchain)
                }.toSet()
            }
            is UserWallet.Hot -> {
                networks.toSet() // TODO [REDACTED_TASK_KEY]
            }
        }
    }
}