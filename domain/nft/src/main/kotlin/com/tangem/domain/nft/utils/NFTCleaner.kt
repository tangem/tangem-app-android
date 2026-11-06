package com.tangem.domain.nft.utils

import com.tangem.domain.common.wallets.CryptoCurrencyDataCleaner
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Cleans up NFT data for a given user wallet and network(s).
 *
[REDACTED_AUTHOR]
 */
interface NFTCleaner : CryptoCurrencyDataCleaner {

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

    override suspend fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        invoke(userWalletId = userWalletId, networks = currencies.mapTo(hashSetOf(), CryptoCurrency::network))
    }

    // NFT data has never been removed on wallet deletion; kept as is to not change behaviour in this migration
    override suspend fun clear(userWalletIds: List<UserWalletId>) = Unit
}