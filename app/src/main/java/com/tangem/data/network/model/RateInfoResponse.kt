package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class RateInfoResponse(
        @SerializedName("data")
        var data: RateData? = null
)

data class RateData(
        @SerializedName("quote")
        var quote: Quote? = null
)

data class Quote(
        @SerializedName("USD")
        var usd: CurrencyRate? = null
)

data class CurrencyRate(
        @SerializedName("price")
        var price: Float? = null
)