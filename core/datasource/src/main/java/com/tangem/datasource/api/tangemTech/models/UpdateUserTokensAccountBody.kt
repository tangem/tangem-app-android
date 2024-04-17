package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json

data class UpdateUserTokensAccountBody(
    @Json(name = "accountTitle")
    val title: String,
)
