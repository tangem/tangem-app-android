package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property id        the invite code, 64 upper-case hex chars, returned nowhere else
 * @property expiresAt ISO 8601 by the server clock
 */
@JsonClass(generateAdapter = true)
data class JointAccountIssuedInviteDto(
    @Json(name = "id") val id: String,
    @Json(name = "slot") val slot: Int,
    @Json(name = "expiresAt") val expiresAt: String,
)