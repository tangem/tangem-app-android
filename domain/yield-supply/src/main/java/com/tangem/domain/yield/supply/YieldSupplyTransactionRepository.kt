package com.tangem.domain.yield.supply

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.GaslessYieldRepository
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
}