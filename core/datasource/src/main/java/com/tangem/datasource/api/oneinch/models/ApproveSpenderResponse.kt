package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Approve spender response
 *
 * @property address Address of the 1inch router that must be trusted to spend funds for the exchange
 */
data class ApproveSpenderResponse(
    @Json(name = "address") val address: String,
)
