package com.tangem.domain.networks.repository

import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Repository for working with pending transactions
 *
[REDACTED_AUTHOR]
 */
interface NetworksRepository {

    /** Fetches pending transactions for given [network] in selected [userWalletId] */
    suspend fun fetchPendingTransactions(userWalletId: UserWalletId, network: Network)

    /**
     * Returns addresses and crypto currency
     *
     * @param userWalletId the unique identifier of the user wallet
     * @param network      network
     */
    suspend fun getNetworkAddresses(userWalletId: UserWalletId, network: Network): List<CryptoCurrencyAddress>
}