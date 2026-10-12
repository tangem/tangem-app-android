package com.tangem.domain.yield.supply.incentives

import arrow.core.Either
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.models.incentives.YieldClaimHistoryItem
import com.tangem.domain.yield.supply.models.incentives.YieldIncentives
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError

/**
 * Access to the Merkl incentives overlay of Yield Mode.
 *
 * Every method is total: transport, parsing and toggle-off all surface as a [YieldIncentivesError] left.
 */
interface YieldIncentivesRepository {

    /** Yield markets with the incentive overlay. Cached for [INCENTIVE_MARKETS_CACHE_SECONDS] seconds. */
    suspend fun getIncentiveMarkets(chainIds: List<Int>): Either<YieldIncentivesError, List<YieldMarketToken>>

    /** Rewards of the user's module on [chainId]. Cached for [INCENTIVES_CACHE_SECONDS] seconds. */
    suspend fun getIncentives(chainId: Int, userAddress: String): Either<YieldIncentivesError, YieldIncentives>

    /**
     * Daily incentivised APY history of [tokenAddress].
     *
     * @param days bounded by the backend to 1..90; values outside the range are coerced.
     */
    suspend fun getAprHistory(
        chainId: Int,
        tokenAddress: String,
        days: Int,
    ): Either<YieldIncentivesError, YieldAprHistory>

    /** Payload of the owner-signed claim transaction. Always fetched from the network, never cached. */
    suspend fun getClaimCalldata(chainId: Int, userAddress: String): Either<YieldIncentivesError, YieldClaimCalldata>

    /** Claims the backend tracker detected. Cached for [CLAIM_HISTORY_CACHE_SECONDS] seconds. */
    suspend fun getClaimHistory(
        chainId: Int,
        userAddress: String,
    ): Either<YieldIncentivesError, List<YieldClaimHistoryItem>>

    companion object {
        const val MIN_APR_HISTORY_DAYS = 1
        const val MAX_APR_HISTORY_DAYS = 90
        const val INCENTIVE_MARKETS_CACHE_SECONDS = 30
        const val INCENTIVES_CACHE_SECONDS = 30
        const val APR_HISTORY_CACHE_SECONDS = 3600
        const val CLAIM_HISTORY_CACHE_SECONDS = 60
    }
}