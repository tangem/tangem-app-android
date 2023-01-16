package com.tangem.tap.persistence

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.tangem.tap.common.analytics.events.AnalyticsParam

class CardBalanceStateAdapter {

    @ToJson
    fun toJson(src: AnalyticsParam.CardBalanceState): String = src.value

    @FromJson
    fun fromJson(json: String): AnalyticsParam.CardBalanceState {
        return when (json) {
            AnalyticsParam.CardBalanceState.Empty.value -> AnalyticsParam.CardBalanceState.Empty
            AnalyticsParam.CardBalanceState.Full.value -> AnalyticsParam.CardBalanceState.Full
            else -> error("CardBalanceState not found")
        }
    }
}
