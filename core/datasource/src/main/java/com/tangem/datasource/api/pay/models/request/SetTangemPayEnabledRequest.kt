package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetTangemPayEnabledRequest(
    @Json(name = "is_tangem_pay_enabled") val isTangemPayEnabled: Boolean,
)