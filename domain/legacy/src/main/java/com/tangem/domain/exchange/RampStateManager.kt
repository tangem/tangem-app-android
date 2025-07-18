package com.tangem.domain.exchange

import arrow.core.Either
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Manager that holds info about available actions as Sell and Buy
 */
interface RampStateManager {

    suspend fun availableForBuy(userWallet: UserWallet, cryptoCurrency: CryptoCurrency): ScenarioUnavailabilityReason

    /**
     * Check if [CryptoCurrency] is available for sell
     *
     * @param userWalletId             the ID of the user's wallet
     * @param status                   crypto currency status
     * @param sendUnavailabilityReason the reason why sending is unavailable or null
     */
    suspend fun availableForSell(
        userWalletId: UserWalletId,
        status: CryptoCurrencyStatus,
        sendUnavailabilityReason: ScenarioUnavailabilityReason?,
    ): Either<ScenarioUnavailabilityReason, Unit>

    suspend fun availableForSwap(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ScenarioUnavailabilityReason

    suspend fun fetchSellServiceData()

    fun getSellInitializationStatus(): Flow<Lce<Throwable, Any>>

    fun getExpressInitializationStatus(userWalletId: UserWalletId): Flow<Lce<Throwable, Any>>

    /**
     * Returns the reason why sending is unavailable for the given user wallet and cryptocurrency status
     *
     * @param userWalletId         the ID of the user's wallet
     * @param cryptoCurrencyStatus the status of the cryptocurrency
     */
    suspend fun getSendUnavailabilityReason(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): ScenarioUnavailabilityReason
}