package com.tangem.domain.staking.utils

import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Cleans up staking-related data for a specific user wallet and a set of staking IDs.
 *
[REDACTED_AUTHOR]
 */
interface StakingCleaner {

    /**
     * Cleans up staking-related data for the given [userWalletId] and set of [stakingIds].
     *
     * @param userWalletId The ID of the user wallet for which to clean up data.
     * @param stakingIds The set of staking IDs whose associated data should be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, stakingIds: Set<StakingID>)
}