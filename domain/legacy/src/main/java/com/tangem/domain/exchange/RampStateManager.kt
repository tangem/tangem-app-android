package com.tangem.domain.exchange

import arrow.core.Either
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Manager that holds info about available actions as Sell and Buy
 */
interface RampStateManager {

    suspend fun availableForBuy(
        scanResponse: ScanResponse,
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ScenarioUnavailabilityReason

    /**
     * Check if [CryptoCurrency] is available for sell
     *
     * @param userWallet user wallet
     * @param status     crypto currency status
     */
    suspend fun availableForSell(
        userWallet: UserWallet,
        status: CryptoCurrencyStatus,
    ): Either<ScenarioUnavailabilityReason, Unit>

    suspend fun availableForSwap(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ScenarioUnavailabilityReason

    suspend fun fetchSellServiceData()

    fun getSellInitializationStatus(): Flow<Lce<Throwable, Any>>

    fun getExpressInitializationStatus(userWalletId: UserWalletId): Flow<Lce<Throwable, Any>>
}