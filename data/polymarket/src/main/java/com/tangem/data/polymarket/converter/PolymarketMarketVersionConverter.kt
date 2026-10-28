package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.utils.converter.Converter

internal object PolymarketMarketVersionConverter : Converter<String?, PolymarketMarketVersion> {

    private const val VERSION_V1 = "v1"
    private const val VERSION_V2 = "v2"

    override fun convert(value: String?): PolymarketMarketVersion = when (value) {
        VERSION_V1 -> PolymarketMarketVersion.V1
        VERSION_V2 -> PolymarketMarketVersion.V2
        else -> PolymarketMarketVersion.UNKNOWN
    }
}