package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.utils.converter.Converter

internal object PolymarketStatusConverter : Converter<String, PolymarketStatus> {
    override fun convert(value: String): PolymarketStatus {
        return when (value) {
            "active" -> PolymarketStatus.ACTIVE
            "closed" -> PolymarketStatus.CLOSED
            "archived" -> PolymarketStatus.ARCHIVED
            else -> PolymarketStatus.UNKNOWN
        }
    }
}