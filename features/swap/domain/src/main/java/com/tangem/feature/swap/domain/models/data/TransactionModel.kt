package com.tangem.feature.swap.domain.models.data

/**
 * Transaction model
 *
 * @property fromWalletAddress wallet from address (transactions will be sent from this address)
 * @property toWalletAddress transactions will be sent to our(1inch) contract address
 * @property data The encoded data to call the approve method on the swapped token contract
 * @property value  Native token value in WEI (for approve is always 0)
 * @property gasPrice maximum amount of gas for a swap default: 11500000; max: 11500000
 * @property gas estimated amount of the gas limit, increase this value by 25%
 */
data class TransactionModel(
    val fromWalletAddress: String,
    val toWalletAddress: String,
    val data: String,
    val value: String,
    val gasPrice: String,
    val gas: String,
)
