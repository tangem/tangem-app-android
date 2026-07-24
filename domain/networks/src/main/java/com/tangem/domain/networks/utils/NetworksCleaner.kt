package com.tangem.domain.networks.utils

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Cleans up network-related data for a specific user wallet and a list of cryptocurrencies.
 *
[REDACTED_AUTHOR]
 */
interface NetworksCleaner {

    /**
     * Cleans up network-related data for the given [userWalletId] and [currency].
     *
     * @param userWalletId The ID of the user wallet for which to clean up data.
     * @param currency The cryptocurrency whose associated network data should be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency) {
        invoke(userWalletId = userWalletId, currencies = listOf(currency))
    }

    /**
     * Cleans up network-related data for the given [userWalletId] and list of [currencies].
     *
     * @param userWalletId The ID of the user wallet for which to clean up data.
     * @param currencies The list of cryptocurrencies whose associated network data should be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    /**
     * Removes all cached network data for the given [userWalletIds].
     *
     * Used on wallet deletion, where the concrete networks are no longer available.
     *
     * @param userWalletIds The IDs of the user wallets whose network data should be removed.
     */
    suspend fun clear(userWalletIds: List<UserWalletId>)
}