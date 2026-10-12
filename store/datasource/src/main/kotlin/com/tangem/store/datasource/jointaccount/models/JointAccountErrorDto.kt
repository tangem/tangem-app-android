package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The body of a refused joint-account call.
 *
 * @property code names the reason the backend refused, e.g. `NONCE_MISMATCH`; absent on a refusal that carries
 * no code of its own
 */
@JsonClass(generateAdapter = true)
data class JointAccountErrorDto(
    @Json(name = "code") val code: String? = null,
)