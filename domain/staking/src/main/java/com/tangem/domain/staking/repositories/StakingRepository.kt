package com.tangem.domain.staking.repositories

import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.staking.model.*
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface StakingRepository {

    fun isStakingSupported(currencyId: String): Boolean

    suspend fun fetchEnabledYields(refresh: Boolean)

    suspend fun getEntryInfo(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): StakingEntryInfo

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

    suspend fun createAction(params: ActionParams): StakingAction

    suspend fun estimateGas(params: ActionParams): StakingGasEstimate

    suspend fun constructTransaction(transactionId: String): StakingTransaction

    suspend fun submitHash(transactionId: String, transactionHash: String)

    suspend fun storeUnsubmittedHash(unsubmittedTransactionMetadata: UnsubmittedTransactionMetadata)

    suspend fun sendUnsubmittedHashes()

    /** Returns whether additional staking is possible if there is already active staking */
    fun isStakeMoreAvailable(networkId: Network.ID): Boolean
}
