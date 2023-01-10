package com.tangem.datasource.api.paymentology.models.response

import com.squareup.moshi.Json

data class RegisterWalletResponse(
    @Json(name = "success") override val success: Boolean,
    @Json(name = "error") override val error: String?,
    @Json(name = "errorCode") override val errorCode: Int?,
) : ResponseError
