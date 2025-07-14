package com.tangem.domain.swap.models

/**
 * Refund data
 *
 * @param refundAddress address refund send to
 * @param refundExtraId refund token id
 */
data class SwapRefundData(
    val refundAddress: String? = null,
    val refundExtraId: String? = null,
)