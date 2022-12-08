package com.tangem.feature.swap.domain.models.data

/**
 * Approve model
 *
 * @property data The encoded data to call the approve method on the swapped token contract
 * @property gasPrice Gas price for fast transaction processing
 * @property toAddress Token address that will be allowed to exchange through 1inch router
 */
data class ApproveModel(
    val data: String,
    val gasPrice: String,
    val toAddress: String,
)
