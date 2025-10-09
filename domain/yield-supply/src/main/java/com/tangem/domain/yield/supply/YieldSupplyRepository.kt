package com.tangem.domain.yield.supply

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import kotlinx.coroutines.flow.Flow

interface YieldSupplyRepository {

    /**
     * Get cached yield markets or null if nothing cached yet.
     */
    suspend fun getCachedMarkets(): List<YieldMarketToken>?

    /**
     * Update markets by fetching from network and cache the result. Returns latest markets.
     */
    @Throws
    suspend fun updateMarkets(): List<YieldMarketToken>

    /**
     * Observe runtime markets updates.
     */
    fun getMarketsFlow(): Flow<List<YieldMarketToken>>

    /**
     * Get yield token status by contract address from cache
     */
    @Throws
    suspend fun getTokenStatus(cryptoCurrencyToken: CryptoCurrency.Token): YieldMarketToken

    /**
     * Get yield token APY chart by contract address.
     */
    @Throws
    suspend fun getTokenChart(cryptoCurrencyToken: CryptoCurrency.Token): YieldSupplyMarketChartData

    suspend fun isYieldSupplySupported(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean

    /**
     * Activate yield protocol for the specified token.
     *
     * Returns whether the token is active after the operation completes.
     * May throw on network/backend errors or if required chain id cannot be resolved.
     */
    @Throws
    suspend fun activateProtocol(cryptoCurrencyToken: CryptoCurrency.Token): Boolean

    /**
     * Deactivate yield protocol for the specified token.
     *
     * Returns whether the token is active after the operation completes
     * (expected to be false when deactivation succeeds). May throw on
     * network/backend errors or if required chain id cannot be resolved.
     */
    @Throws
    suspend fun deactivateProtocol(cryptoCurrencyToken: CryptoCurrency.Token): Boolean
}