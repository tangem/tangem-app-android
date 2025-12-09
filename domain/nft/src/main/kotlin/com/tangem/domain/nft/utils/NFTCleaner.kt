package com.tangem.domain.nft.utils

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Cleans up NFT data for a given user wallet and network(s).
 *
[REDACTED_AUTHOR]
 */
interface NFTCleaner {

    /**
     * Cleans up NFT data for a given user wallet and single network.
     *
     * @param userWalletId the user wallet id
     * @param network the network to clean up
     */
    suspend operator fun invoke(userWalletId: UserWalletId, network: Network) {
        invoke(userWalletId = userWalletId, networks = setOf(network))
    }

    /**
     * Cleans up NFT data for a given user wallet and multiple networks.
     *
     * @param userWalletId the user wallet id
     * @param networks the set of networks to clean up
     */
    suspend operator fun invoke(userWalletId: UserWalletId, networks: Set<Network>)
}