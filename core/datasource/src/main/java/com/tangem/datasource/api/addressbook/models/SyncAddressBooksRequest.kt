package com.tangem.datasource.api.addressbook.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /address-books/sync`.
 *
 * Each [Wallet.etag] is optional: when it matches the backend's etag, that wallet is omitted from the
 * response and the local copy is kept.
 */
@JsonClass(generateAdapter = true)
data class SyncAddressBooksRequest(
    @Json(name = "wallets") val wallets: List<Wallet>,
) {

    @JsonClass(generateAdapter = true)
    data class Wallet(
        @Json(name = "walletId") val walletId: String,
        @Json(name = "etag") val etag: String? = null,
    )
}