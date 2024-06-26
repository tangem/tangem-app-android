package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.Token
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.staking.model.action.EnterAction
import com.tangem.domain.staking.model.transaction.StakingTransaction
import java.math.BigDecimal
import com.tangem.domain.staking.model.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface StakingRepository {

    fun isStakingSupported(currencyId: String): Boolean

    suspend fun fetchEnabledYields()

    suspend fun getEntryInfo(integrationId: String): StakingEntryInfo

    suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield

    suspend fun getStakingAvailabilityForActions(
        cryptoCurrencyId: CryptoCurrency.ID,
        symbol: String,
    ): StakingAvailability

    suspend fun fetchSingleYieldBalance(
        userWalletId: UserWalletId,
        networkStatus: NetworkStatus,
        integrationId: String,
        refresh: Boolean = false,
    )

    fun getSingleYieldBalanceFlow(
        userWalletId: UserWalletId,
        networkStatus: NetworkStatus,
        integrationId: String,
    ): Flow<List<YieldBalance>>

    suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        networks: Set<NetworkStatus>,
        integrationId: String,
        refresh: Boolean = false,
    )

    fun getMultiYieldBalanceFlow(
        userWalletId: UserWalletId,
        networks: Set<NetworkStatus>,
        integrationId: String,
    ): Flow<List<YieldBalanceList>>

    suspend fun createEnterAction(
        integrationId: String,
        amount: BigDecimal,
        address: String,
        validatorAddress: String,
        token: Token,
    ): EnterAction

    suspend fun constructTransaction(transactionId: String): StakingTransaction
}
