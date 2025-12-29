package com.tangem.data.staking.store

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Store for P2PEthPool staking balances.
 *
 * Extends [BaseStakingBalancesStore] with P2PEthPool-specific storage operations.
 */
interface P2PEthPoolBalancesStore : BaseStakingBalancesStore {

    /** Store actual P2PEthPool account balances */
    suspend fun storeActual(userWalletId: UserWalletId, values: Set<P2PEthPoolAccountResponse>)

    /** Store empty state for accounts with no active positions */
    suspend fun storeEmpty(userWalletId: UserWalletId, stakingIds: Set<StakingID>)
}