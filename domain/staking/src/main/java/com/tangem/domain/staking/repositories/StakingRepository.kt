package com.tangem.domain.staking.repositories

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface StakingRepository {

    fun getIntegrationKey(cryptoCurrencyId: CryptoCurrency.ID): String

    fun isStakingSupported(integrationKey: String): Boolean

    suspend fun fetchEnabledYields(refresh: Boolean)

    suspend fun getEntryInfo(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): StakingEntryInfo

    suspend fun getYield(cryptoCurrencyId: CryptoCurrency.ID, symbol: String): Yield

    suspend fun getStakingAvailability(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): StakingAvailability

    suspend fun fetchSingleYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        refresh: Boolean = false,
    )

    fun getSingleYieldBalanceFlow(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Flow<YieldBalance>

    suspend fun getSingleYieldBalanceSync(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): YieldBalance

    suspend fun fetchMultiYieldBalance(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
        refresh: Boolean = false,
    )

    fun getMultiYieldBalanceFlow(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Flow<YieldBalanceList>

    fun getMultiYieldBalanceLce(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): LceFlow<Throwable, YieldBalanceList>

    suspend fun getMultiYieldBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): YieldBalanceList

    suspend fun createAction(userWalletId: UserWalletId, network: Network, params: ActionParams): StakingAction

    suspend fun estimateGas(userWalletId: UserWalletId, network: Network, params: ActionParams): StakingGasEstimate

    suspend fun constructTransaction(
        networkId: String,
        fee: Fee,
        transactionId: String,
    ): Pair<StakingTransaction, TransactionData.Compiled>

    suspend fun submitHash(transactionId: String, transactionHash: String)

    suspend fun storeUnsubmittedHash(unsubmittedTransactionMetadata: UnsubmittedTransactionMetadata)

    suspend fun sendUnsubmittedHashes()

    /** Returns staking approval */
    fun getStakingApproval(cryptoCurrency: CryptoCurrency): StakingApproval
}
