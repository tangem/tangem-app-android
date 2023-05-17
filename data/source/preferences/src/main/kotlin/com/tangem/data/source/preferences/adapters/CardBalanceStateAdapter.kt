package com.tangem.data.source.preferences.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.tangem.data.source.preferences.model.DataSourceTopupInfo

class CardBalanceStateAdapter {

    @ToJson
    fun toJson(src: DataSourceTopupInfo.CardBalanceState): String = src.serializedName

    @FromJson
    fun fromJson(json: String): DataSourceTopupInfo.CardBalanceState {
        return when (json) {
            DataSourceTopupInfo.CardBalanceState.Empty.serializedName -> DataSourceTopupInfo.CardBalanceState.Empty
            DataSourceTopupInfo.CardBalanceState.Full.serializedName -> DataSourceTopupInfo.CardBalanceState.Full
            else -> error("CardBalanceState not found")
        }
    }
}