package com.tangem.store.preferences.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.tangem.store.preferences.model.TopupInfoDM

class CardBalanceStateAdapter {

    @ToJson
    fun toJson(src: TopupInfoDM.CardBalanceState): String = src.serializedName

    @FromJson
    fun fromJson(json: String): TopupInfoDM.CardBalanceState {
        return when (json) {
            TopupInfoDM.CardBalanceState.Empty.serializedName -> TopupInfoDM.CardBalanceState.Empty
            TopupInfoDM.CardBalanceState.Full.serializedName -> TopupInfoDM.CardBalanceState.Full
            else -> error("CardBalanceState not found")
        }
    }
}
