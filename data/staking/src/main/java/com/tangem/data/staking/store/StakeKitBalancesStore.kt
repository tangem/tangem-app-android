package com.tangem.data.staking.store

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Store for StakeKit staking balances.
 *
 * Extends [BaseStakingBalancesStore] with StakeKit-specific storage operations.
 */
interface StakeKitBalancesStore : BaseStakingBalancesStore {

    /** Store actual StakeKit yield balances */
    suspend fun storeActual(userWalletId: UserWalletId, values: Set<YieldBalanceWrapperDTO>)
}