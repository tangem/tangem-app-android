package com.tangem.domain.staking.utils

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Cleans up staking-related data for a specific user wallet and a set of staking IDs.
 *
[REDACTED_AUTHOR]
 */
interface StakingCleaner {

    /**
     * Cleans up staking-related data for the given [userWalletId] and single [currency].
     *
     * @param userWalletId The ID of the user wallet for which to clean up data.
     * @param currency The cryptocurrency whose associated staking data should be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency) {
        invoke(userWalletId = userWalletId, currencies = listOf(currency))
    }

    /**
     * Cleans up staking-related data for the given [userWalletId] and list of [currencies].
     *
     * @param userWalletId The ID of the user wallet for which to clean up data.
     * @param currencies The list of cryptocurrencies whose associated staking data should be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    /**
     * Cleans up staking-related data for the given [userWalletId] and single [stakingId].
     *
     * @param userWalletId The ID of the user wallet for which to clean up data.
     * @param stakingId The staking ID whose associated data should be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, stakingId: StakingID) {
        invoke(userWalletId = userWalletId, stakingIds = setOf(stakingId))
    }

    /**
     * Cleans up staking-related data for the given [userWalletId] and set of [stakingIds].
     *
     * @param userWalletId The ID of the user wallet for which to clean up data.
     * @param stakingIds The set of staking IDs whose associated data should be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, stakingIds: Set<StakingID>)
}