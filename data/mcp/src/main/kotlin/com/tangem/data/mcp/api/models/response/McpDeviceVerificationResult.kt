package com.tangem.data.mcp.api.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Outcome of approving a device verification. */
@Serializable
internal data class McpDeviceVerificationResult(
    @SerialName("success") val isSuccess: Boolean,
)