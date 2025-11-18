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
     * Cleans up network-related data for the given [userWalletId] and list of [currencies].
     *
     * @param userWalletId The ID of the user wallet for which to clean up data.
     * @param currencies The list of cryptocurrencies whose associated network data should be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)
}