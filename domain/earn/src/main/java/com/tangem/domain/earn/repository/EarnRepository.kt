package com.tangem.domain.earn.repository

import com.tangem.domain.earn.model.EarnTokensBatchFlow
import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.domain.models.earn.EarnTopToken
import kotlinx.coroutines.flow.Flow

interface EarnRepository {

    fun getEarnTokensBatchFlow(context: EarnTokensBatchingContext, batchSize: Int): EarnTokensBatchFlow

    /**
     * Load all networks for Earn and store it in data store.
     */
    suspend fun fetchEarnNetworks(type: String)

    fun observeEarnNetworks(): Flow<EarnNetworks?>

    /**
     * Fetch top N earn tokens by isForEarn = true and hold it in runtime store.
     */
    suspend fun fetchTopEarnTokens(limit: Int)

    fun observeTopEarnTokens(): Flow<EarnTopToken?>
}