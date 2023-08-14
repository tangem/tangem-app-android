package com.tangem.datasource.local.quote.model

import com.squareup.moshi.Json
import com.tangem.datasource.api.tangemTech.models.QuotesResponse

data class StoredQuote(
    @Json(name = "rawCurrencyId") val rawCurrencyId: String,
    @Json(name = "quote") val quote: QuotesResponse.Quote,
)
