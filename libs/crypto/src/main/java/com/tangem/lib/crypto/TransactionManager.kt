package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.*
import java.math.BigDecimal

interface TransactionManager {

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
    suspend fun updateWalletManager(networkId: String, derivationPath: String?)

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
