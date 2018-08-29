package com.tangem.data.network.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RateInfoModel(
        @SerializedName(ID)
        var id: String = "",

        @SerializedName(NAME)
        var name: String = "",

        @SerializedName(SYMBOL)
        var symbol: String = "",

        @SerializedName(RANK)
        var rank: String = "",

        @SerializedName(PRICE_USD)
        var priceUsd: String = "",

        @SerializedName(PRICE_BTC)
        var priceBtc: String = "",

        @SerializedName(VOLUME_USD_24H)
        var volumeUsd24h: String = "",

        @SerializedName(MARKET_CAP_USD)
        var marketCapUsd: String = "",

        @SerializedName(AVAILABLE_SUPPLY)
        var availableSupply: String = "",

        @SerializedName(TOTAL_SUPPLY)
        var totalSupply: String = "",

        @SerializedName(MAX_SUPPLY)
        var maxSupply: String = "",

        @SerializedName(PERCENT_CHANGE_1H)
        var percentChange1h: String = "",

        @SerializedName(PERCENT_CHANGE_24H)
        var percentChange24h: String = "",

        @SerializedName(PERCENT_CHANGE_7H)
        var percentChange7h: String = "",

        @SerializedName(LAST_UPDATED)
        var lastUpdated: String = ""

) : Parcelable {

    companion object {
        val TAG: String = RateInfoModel::class.java.simpleName

        const val ID = "id"
        const val NAME = "name"
        const val SYMBOL = "symbol"
        const val RANK = "rank"
        const val PRICE_USD = "price_usd"
        const val PRICE_BTC = "price_btc"
        const val VOLUME_USD_24H = "24h_volume_usd"
        const val MARKET_CAP_USD = "market_cap_usd"
        const val AVAILABLE_SUPPLY = "available_supply"
        const val TOTAL_SUPPLY = "total_supply"
        const val MAX_SUPPLY = "max_supply"
        const val PERCENT_CHANGE_1H = "percent_change_1h"
        const val PERCENT_CHANGE_24H = "percent_change_24h"
        const val PERCENT_CHANGE_7H = "percent_change_7d"
        const val LAST_UPDATED = "last_updated"
    }

}