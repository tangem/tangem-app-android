package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.ProxyFees
import com.tangem.lib.crypto.models.ProxyNetworkInfo
import com.tangem.lib.crypto.models.transactions.SendTxResult
import java.math.BigDecimal

interface TransactionManager {

    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun sendApproveTransaction(
        networkId: String,
        feeAmount: BigDecimal,
        gasLimit: Int,
        destinationAddress: String,
        dataToSign: String,
        derivationPath: String?,
    ): SendTxResult

    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun sendTransaction(
        networkId: String,
        amountToSend: BigDecimal,
        feeAmount: BigDecimal,
        gasLimit: Int,
        destinationAddress: String,
        dataToSign: String,
        isSwap: Boolean,
        currencyToSend: Currency,
        derivationPath: String?,
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
        amountToSend: BigDecimal,
        currencyToSend: Currency,
        destinationAddress: String,
        increaseBy: Int?,
        data: String?,
        derivationPath: String?,
    ): ProxyFees

    @Throws(IllegalStateException::class)
    fun getNativeTokenDecimals(networkId: String): Int

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
}