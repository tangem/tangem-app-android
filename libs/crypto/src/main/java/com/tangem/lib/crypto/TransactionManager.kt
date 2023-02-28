package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.ProxyNetworkInfo
import com.tangem.lib.crypto.models.transactions.SendTxResult
import java.math.BigDecimal

interface TransactionManager {

    @Throws(IllegalStateException::class)
    suspend fun sendApproveTransaction(
        networkId: String,
        feeAmount: BigDecimal,
        estimatedGas: Int,
        destinationAddress: String,
        dataToSign: String,
    ): SendTxResult

    @Suppress("LongParameterList")
    @Throws(IllegalStateException::class)
    suspend fun sendTransaction(
        networkId: String,
        amountToSend: BigDecimal,
        feeAmount: BigDecimal,
        estimatedGas: Int,
        destinationAddress: String,
        dataToSign: String,
        isSwap: Boolean,
        currencyToSend: Currency,
    ): SendTxResult

    @Throws(IllegalStateException::class)
    suspend fun getFee(
        networkId: String,
        amountToSend: BigDecimal,
        currencyToSend: Currency,
        destinationAddress: String,
    ): ProxyAmount

    @Throws(IllegalStateException::class)
    fun getNativeTokenDecimals(networkId: String): Int

    @Throws(IllegalStateException::class)
    suspend fun updateWalletManager(networkId: String)

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
