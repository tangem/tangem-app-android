package com.tangem.data.polymarket.geo

import com.squareup.moshi.Moshi
import com.tangem.grow.datasource.polymarket.geo.models.PolymarketGeoblockResponse

internal object PolymarketGeoblockParser {

    private val adapter = Moshi.Builder().build().adapter(PolymarketGeoblockResponse::class.java)

    fun parseIsBlocked(body: String): Boolean? = runCatching { adapter.fromJson(body)?.blocked }.getOrNull()
}