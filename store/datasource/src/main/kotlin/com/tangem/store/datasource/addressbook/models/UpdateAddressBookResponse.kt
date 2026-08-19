package com.tangem.store.datasource.addressbook.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response body for `PUT /address-books/{walletId}`. */
@JsonClass(generateAdapter = true)
data class UpdateAddressBookResponse(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "etag") val etag: String,
    @Json(name = "updatedAt") val updatedAt: String,
)