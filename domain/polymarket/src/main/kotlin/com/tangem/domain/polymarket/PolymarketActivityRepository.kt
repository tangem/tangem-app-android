package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.polymarket.model.PolymarketActivityBatchFlow
import com.tangem.domain.polymarket.model.PolymarketActivityBatchingContext
import com.tangem.domain.polymarket.model.PolymarketActivityError
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import com.tangem.domain.polymarket.model.PolymarketActivityPage
import com.tangem.domain.polymarket.model.PolymarketAddresses
import kotlin.time.Duration

interface PolymarketActivityRepository {

    fun getActivityBatchFlow(context: PolymarketActivityBatchingContext, batchSize: Int): PolymarketActivityBatchFlow

    suspend fun getActivity(
        config: PolymarketActivityListConfig,
        cursor: String?,
        limit: Int,
    ): Either<PolymarketActivityError, PolymarketActivityPage>

    /** Starts a deposit scan and returns the service's suggested delay before checking the history again. */
    suspend fun reconcileDeposits(addresses: PolymarketAddresses): Either<PolymarketActivityError, Duration>
}