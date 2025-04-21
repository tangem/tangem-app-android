package com.tangem.data.staking.fetcher

import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.model.StakingID

/**
 * Implementor of internal logic of YieldBalanceFetcher
 *
* [REDACTED_AUTHOR]
 */
internal interface YieldBalanceFetcherImplementor<out Params : YieldBalanceFetcherParams> {

    /** Create set of [StakingID] */
    suspend fun createStakingIds(params: @UnsafeVariance Params): Set<StakingID>

    /**
     * Fetch yield balances
     *
     * @param params     params
     * @param stakingIds set of [StakingID]
     * @param requests   requests
     */
    suspend fun fetch(
        params: @UnsafeVariance Params,
        stakingIds: Set<StakingID>,
        requests: List<YieldBalanceRequestBody>,
    )
}
