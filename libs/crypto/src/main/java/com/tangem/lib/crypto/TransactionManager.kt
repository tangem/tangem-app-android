package com.tangem.lib.crypto

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.lib.crypto.models.*
import com.tangem.lib.crypto.models.transactions.SendTxResult
import java.math.BigDecimal
import java.math.BigInteger

interface TransactionManager {

    @Throws(IllegalStateException::class)
    suspend fun sendApproveTransaction(
        txData: ApproveTxData,
        derivationPath: String?,
        analyticsData: AnalyticsData,
    ): SendTxResult

    /**
     * Send transaction
     *
     * @param txData data to build a tx
     * @param derivationPath for select right walletManager
     * @param analyticsData data for send analytics event
     * @return result of transaction
     */
    @Throws(IllegalStateException::class)
    suspend fun sendTransaction(
        txData: SwapTxData,
        isSwap: Boolean,
        derivationPath: String?,
        analyticsData: AnalyticsData,
    ): SendTxResult

    /**
     * Get fee
     *
     * @param networkId network id of blockchain
     * @param amountToSend amount
     * @param currencyToSend currency to send in tx
     * @param destinationAddress address to send tx
     * @param increaseBy percents in format 125 = 25%
     * @param data data for tx
     * @param derivationPath derivation path
     * @return
     */
    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun getFee(
        networkId: String,
        amountToSend: Amount,
        currencyToSend: Currency,
        destinationAddress: String,
        increaseBy: Int?,
        data: String?,
        derivationPath: String?,
    ): ProxyFees

    @Throws(IllegalStateException::class)
    suspend fun getFeeForGas(networkId: String, gas: BigInteger, derivationPath: String?): ProxyFees

    @Throws(IllegalStateException::class)
    suspend fun updateWalletManager(networkId: String, derivationPath: String?)

    fun calculateFee(networkId: String, gasPrice: String, estimatedGas: Int): BigDecimal

    /**
     * In app blockchain id, actual in blockchain sdk, not the same as networkId
     *
     * workaround till not use backend only and not integrated server vs sdk
     */
    @Throws(IllegalStateException::class)
    fun getBlockchainInfo(networkId: String): ProxyNetworkInfo

    @Throws(IllegalStateException::class)
    fun getExplorerTransactionLink(networkId: String, txAddress: String): String

    // TODO: move to another place to use as in Send feature
    fun getMemoExtras(networkId: String, memo: String?): TransactionExtras?
}