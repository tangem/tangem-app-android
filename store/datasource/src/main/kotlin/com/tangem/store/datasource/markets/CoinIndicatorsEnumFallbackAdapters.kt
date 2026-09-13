package com.tangem.store.datasource.markets

import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.UnknownEnumMoshiAdapter
import com.tangem.store.datasource.markets.models.response.GetCoinIndicatorsResponse.Asset.Indicator

fun Moshi.Builder.addCoinIndicatorsEnumFallbackAdapters(): Moshi.Builder {
    val map = mapOf(
        Indicator.Type::class.java to Indicator.Type.UNKNOWN,
        Indicator.Timeframe::class.java to Indicator.Timeframe.UNKNOWN,
        Indicator.Signal::class.java to Indicator.Signal.UNKNOWN,
    )

    return apply {
        map.forEach { entry ->
            val enumClass = entry.key
            val unknownValue = entry.value
            add(
                enumClass,
                UnknownEnumMoshiAdapter.create(enumClass, unknownValue).nullSafe(),
            )
        }
    }
}