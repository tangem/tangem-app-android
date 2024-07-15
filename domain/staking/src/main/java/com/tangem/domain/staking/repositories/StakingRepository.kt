package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.Token
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.staking.model.action.EnterAction
import com.tangem.domain.staking.model.transaction.StakingTransaction
import java.math.BigDecimal
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.staking.model.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface StakingRepository {

    fun isStakingSupported(currencyId: String): Boolean

    suspend fun fetchEnabledYields(refresh: Boolean)

    suspend fun getEntryInfo(integrationId: String): StakingEntryInfo

    suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield

    suspend fun getStakingAvailabilityForActions(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
    ): StakingAvailability

    suspend fun fetchSingleYieldBalance(
        userWalletId: UserWalletId,
        address: CryptoCurrencyAddress,
        refresh: Boolean = false,
    )

    fun getSingleYieldBalanceFlow(userWalletId: UserWalletId, address: CryptoCurrencyAddress): Flow<YieldBalance>

    suspend fun getSingleYieldBalanceSync(userWalletId: UserWalletId, address: CryptoCurrencyAddress): YieldBalance

    suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
        refresh: Boolean = false,
    )

    fun getMultiYieldBalanceFlow(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
    ): Flow<YieldBalanceList>

    fun getMultiYieldBalanceLce(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
    ): LceFlow<Throwable, YieldBalanceList>

    suspend fun getMultiYieldBalanceSync(
        userWalletId: UserWalletId,
        addresses: List<CryptoCurrencyAddress>,
    ): YieldBalanceList

    suspend fun createEnterAction(
        integrationId: String,
        amount: BigDecimal,
        address: String,
        validatorAddress: String,
        token: Token,
    ): EnterAction

    suspend fun constructTransaction(transactionId: String): StakingTransaction

    suspend fun submitHash(transactionId: String, transactionHash: String)

    suspend fun storeUnsubmittedHash(unsubmittedTransactionMetadata: UnsubmittedTransactionMetadata)

    suspend fun sendUnsubmittedHashes()
}