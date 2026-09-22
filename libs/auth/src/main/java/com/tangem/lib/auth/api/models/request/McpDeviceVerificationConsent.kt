package com.tangem.lib.auth.api.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Consent body for approving a device verification.
 *
 * @property clientId echoed back from [com.tangem.lib.auth.api.models.response.McpDeviceVerificationResponse].
 * @property scope echoed back from the same response — the app never narrows or picks it.
 * @property walletId the wallet the user granted access to, Base64-encoded the way the auth service
 * identifies wallets everywhere else.
 */
@JsonClass(generateAdapter = true)
data class McpDeviceVerificationConsent(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "scope") val scope: String,
    @Json(name = "wallet_id") val walletId: String,
)