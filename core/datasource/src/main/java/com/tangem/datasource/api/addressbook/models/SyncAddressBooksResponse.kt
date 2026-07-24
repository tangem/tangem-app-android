package com.tangem.datasource.api.addressbook.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response body for `POST /address-books/sync`.
 *
 * [items] contains only the wallets whose backend etag differs from the one sent in the request; wallets
 * with a matching etag are omitted and their local copy must be kept.
 */
@JsonClass(generateAdapter = true)
data class SyncAddressBooksResponse(
    @Json(name = "items") val items: List<Item>,
) {

    @JsonClass(generateAdapter = true)
    data class Item(
        @Json(name = "walletId") val walletId: String,
        @Json(name = "etag") val etag: String,
        @Json(name = "version") val version: String,
        @Json(name = "updatedAt") val updatedAt: String,
        @Json(name = "nonce") val nonce: String,
        @Json(name = "ciphertext") val ciphertext: String,
        @Json(name = "authTag") val authTag: String,
    )
}