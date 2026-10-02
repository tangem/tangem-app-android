package com.tangem.data.mcp.api.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Outcome of approving a device verification. */
@JsonClass(generateAdapter = true)
internal data class McpDeviceVerificationResult(
    @Json(name = "success") val isSuccess: Boolean,
)