package com.tangem.domain.staking.repositories

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.NetworkType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface StakingRepository {

    fun getIntegrationKey(cryptoCurrencyId: CryptoCurrency.ID): String

    fun getSupportedIntegrationId(cryptoCurrencyId: CryptoCurrency.ID): String?

    suspend fun fetchEnabledYields()

    suspend fun getEntryInfo(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): StakingEntryInfo

    suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield

    suspend fun getYield(yieldId: String): Yield

    fun getStakingAvailability(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Flow<StakingAvailability>

    suspend fun getStakingAvailabilitySync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): StakingAvailability

    suspend fun getActions(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        networkType: NetworkType,
        stakingActionStatus: StakingActionStatus,
    ): List<StakingAction>

    suspend fun fetchSingleYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        refresh: Boolean = false,
    )

    fun getSingleYieldBalanceFlow(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Flow<YieldBalance>

    suspend fun getSingleYieldBalanceSyncLegacy(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): YieldBalance

    suspend fun getSingleYieldBalanceSync(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): YieldBalance

    suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
        refresh: Boolean = false,
    )

    fun getMultiYieldBalanceUpdates(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Flow<YieldBalanceList>

    suspend fun getMultiYieldBalanceSyncLegacy(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): YieldBalanceList

    suspend fun getMultiYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): YieldBalanceList

    suspend fun createAction(userWalletId: UserWalletId, network: Network, params: ActionParams): StakingAction

    suspend fun estimateGas(userWalletId: UserWalletId, network: Network, params: ActionParams): StakingGasEstimate

    suspend fun constructTransaction(
        networkId: String,
        fee: Fee,
        amount: Amount,
        transactionId: String,
    ): Pair<StakingTransaction, TransactionData.Compiled>

    /** Returns staking approval */
    fun getStakingApproval(cryptoCurrency: CryptoCurrency): StakingApproval

    suspend fun isAnyTokenStaked(userWalletId: UserWalletId): Boolean
}