package com.tangem.datasource.api.addressbook.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for `PUT /address-books/{walletId}`. */
@JsonClass(generateAdapter = true)
data class UpdateAddressBookRequest(
    @Json(name = "version") val version: String,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "ciphertext") val ciphertext: String,
    @Json(name = "authTag") val authTag: String,
)