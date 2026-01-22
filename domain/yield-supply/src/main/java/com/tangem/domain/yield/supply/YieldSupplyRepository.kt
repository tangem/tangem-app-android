package com.tangem.domain.yield.supply

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
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
    suspend fun activateProtocol(
        userWalletId: UserWalletId,
        cryptoCurrencyToken: CryptoCurrency.Token,
        address: String,
    ): Boolean

    /**
     * Deactivate yield protocol for the specified token.
     *
     * Returns whether the token is active after the operation completes
     * (expected to be false when deactivation succeeds). May throw on
     * network/backend errors or if required chain id cannot be resolved.
     */
    @Throws
    suspend fun deactivateProtocol(cryptoCurrencyToken: CryptoCurrency.Token, address: String): Boolean

    /**
     * Save the last user‑initiated yield protocol action for the given wallet and currency.
     *
     * The saved value helps the UI render an intermediate "processing" state while waiting
     * for the definitive protocol status to be fetched from the network. This information
     * is transient (in‑memory only) and is not persisted across app restarts.
     *
     * @param userWalletId the wallet the action was performed with
     * @param cryptoCurrency the currency or token the action relates to
     * @param yieldSupplyPendingStatus the last action intent: [YieldSupplyPendingStatus.Enter] or [YieldSupplyPendingStatus.Exit]
     */
    suspend fun saveTokenProtocolPendingStatus(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldSupplyPendingStatus: YieldSupplyPendingStatus?,
    )

    /**
     * Retrieve the hashes of pending (unconfirmed) transactions for the given wallet and currency.
     *
     * @param userWalletId the wallet to query
     * @param cryptoCurrency the currency containing network information
     * @return list of transaction hashes that are still unconfirmed, or an empty list if none exist
     */
    suspend fun getPendingTxHashes(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): List<String>

    /**
     * Get the last saved user‑initiated yield protocol action for the given wallet and currency, if any.
     *
     * Used to determine whether the UI should display an intermediate "processing" state
     * until the protocol status retrieved from backend reflects the change. The value is
     * transient (in‑memory only) and is not persisted across app restarts.
     *
     * @param userWalletId the wallet to query
     * @param cryptoCurrency the currency or token to query
     * @return the last action intent or null if nothing has been recorded
     */
    fun getTokenProtocolPendingStatus(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): YieldSupplyPendingStatus?

    fun getShouldShowYieldPromoBanner(): Flow<Boolean>

    suspend fun setShouldShowYieldPromoBanner(shouldShow: Boolean)
}