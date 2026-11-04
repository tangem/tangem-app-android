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
     * transaction moves no tokens of its own and its amount is always zero. An outdated module has no claim function,
     * so with the yield module V3 toggle on the claim is sent inside the module upgrade.
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

    /**
     * Builds the owner-signed transaction that supplies [amount] of the native coin of [network]: the coin goes in the
     * transaction value and the module wraps it. Until the module is deployed it is sent to the calculated module
     * address, so it has to go after the deployment. An outdated module is upgraded within the same call, since only
     * the latest implementation accepts the coin.
     *
     * @throws com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException if the module is
     * outdated and cannot be upgraded.
     * @throws com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException if the module
     * version cannot be read.
     */
    suspend fun createNativeEnterTransaction(
        userWalletId: UserWalletId,
        network: Network,
        amount: BigDecimal,
    ): TransactionData.Uncompiled

    /**
     * Builds the owner-signed transaction that withdraws [amount] of the native coin of [network] from the module, or
     * the whole position when [amount] is null, which also deactivates the wrapped coin in the module. Upgrades an
     * outdated module the same way as [createNativeEnterTransaction] and throws the same exceptions.
     */
    suspend fun createNativeWithdrawTransaction(
        userWalletId: UserWalletId,
        network: Network,
        amount: BigDecimal?,
    ): TransactionData.Uncompiled

    /**
     * Whether new deposits of [token] into the module are suspended; false when the module is not deployed or predates
     * the suspension. Throws when it cannot be read.
     */
    suspend fun isEntrySuspended(userWalletId: UserWalletId, token: CryptoCurrency.Token): Boolean

    /** How much of [token] the Aave pool can pay out right now, which caps a withdrawal. Throws when it cannot be read. */
    suspend fun getWithdrawLiquidity(userWalletId: UserWalletId, token: CryptoCurrency.Token): BigDecimal

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