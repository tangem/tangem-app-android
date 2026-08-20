package com.tangem.datasource.api.auth.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wallet unregister request — unbinds a wallet from the authenticated device.
 *
 * On success the server returns
 * [com.tangem.datasource.api.auth.models.response.TokenApiResponse] (a fresh session token pair
 * reflecting the updated set of bound wallets).
 */
@JsonClass(generateAdapter = true)
data class WalletUnregisterRequest(
    /** Base64-encoded wallet identifier to unbind. */
    @Json(name = "walletId") val walletId: String,
)