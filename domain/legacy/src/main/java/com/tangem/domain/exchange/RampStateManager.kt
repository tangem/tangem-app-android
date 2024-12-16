package com.tangem.domain.exchange

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Manager that holds info about available actions as Sell and Buy
 */
interface RampStateManager {

    /** Check if sell service is supported for given [CryptoCurrency] */
    fun isSellSupportedByService(cryptoCurrency: CryptoCurrency): Boolean

    suspend fun availableForBuy(
        scanResponse: ScanResponse,
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Boolean

    /**
     * Check if [CryptoCurrency] is available for sell
     *
     * @param userWalletId id of multi-currency wallet
     * @param status       crypto currency status
     */
    suspend fun availableForSell(userWalletId: UserWalletId, status: CryptoCurrencyStatus): Boolean

    suspend fun availableForSwap(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean

    suspend fun fetchBuyServiceData()

    fun getBuyInitializationStatus(): Flow<Lce<Throwable, Any>>

    suspend fun fetchSellServiceData()

    fun getSellInitializationStatus(): Flow<Lce<Throwable, Any>>

    fun getExpressInitializationStatus(userWalletId: UserWalletId): Flow<Lce<Throwable, Any>>
}