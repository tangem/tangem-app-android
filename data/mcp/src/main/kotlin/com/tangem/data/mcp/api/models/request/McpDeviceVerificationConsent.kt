package com.tangem.data.mcp.api.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Consent body for approving a device verification.
 *
 * @property clientId echoed back from [com.tangem.lib.auth.api.models.response.McpDeviceVerificationResponse].
 * @property scope echoed back from the same response — the app never narrows or picks it.
 * @property walletId the wallet the user granted access to, Base64-encoded the way the auth service
 * identifies wallets everywhere else.
 */
@Serializable
internal data class McpDeviceVerificationConsent(
    @SerialName("client_id") val clientId: String,
    @SerialName("scope") val scope: String,
    @SerialName("wallet_id") val walletId: String,
)