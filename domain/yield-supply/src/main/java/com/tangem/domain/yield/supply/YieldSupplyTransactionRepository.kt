package com.tangem.domain.yield.supply

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.yield.supply.models.YieldSupplyEnterOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import java.math.BigDecimal

interface YieldSupplyTransactionRepository : GaslessYieldRepository {

    suspend fun createEnterTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        maxNetworkFee: BigDecimal,
    ): List<TransactionData.Uncompiled>

    suspend fun createExitTransaction(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled

    /**
     * Builds the owner-signed transaction that claims Merkl rewards.
     *
     * [callData] is built by the backend and carries the cumulative proof, so it is passed through untouched; the
     * transaction moves no tokens of its own and its amount is always zero.
     */
    suspend fun createClaimRewardsTransaction(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        destinationAddress: String,
        callData: SmartContractCallData,
        fee: Fee?,
    ): TransactionData.Uncompiled

    /**
     * Checks the version status of the user's yield-module contract and wraps [callData] with an
     * upgrade transaction if the deployed version is out of date.
     */
    suspend fun wrapYieldSwapCallDataWithUpgradeIfNeeded(
        userWalletId: UserWalletId,
        network: Network,
        callData: SmartContractCallData,
    ): SmartContractCallData

    /** Returns the on-chain version status of the user's yield module for [network]. */
    suspend fun getYieldModuleVersionStatus(userWalletId: UserWalletId, network: Network): YieldModuleVersionStatus

    /** @return null until the outcome is known: not mined yet, or the node cannot tell. */
    suspend fun getTransactionOutcome(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
        transactionHash: String,
    ): YieldSupplyTransactionOutcome?

    /**
     * Resolve what happened to the broadcast "enter" transaction [transactionHash].
     *
     * @return null while the transaction has not been mined yet — the outcome is not knowable until then.
     */
    suspend fun getEnterTransactionOutcome(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
        transactionHash: String,
    ): YieldSupplyEnterOutcome?
}