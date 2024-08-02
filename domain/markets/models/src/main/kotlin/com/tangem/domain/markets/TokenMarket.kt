package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenMarket(
    val id: String,
    val name: String,
    val symbol: String,
    val marketRating: Int?,
    val marketCap: BigDecimal?,
    val tokenQuotes: TokenQuotes,
    val tokenCharts: Charts,
    private val imageHost: String,
) {

    data class Charts(
        val h24: TokenChart?,
        val week: TokenChart?,
        val month: TokenChart?,
    )

    // 25x25
    val imageUrlThumb =
        "${imageHost}thumb/$id.png"

    // 50x50
    val imageUrlSmall =
        "${imageHost}small/$id.png"

    // 250x250
    val imageUrlLarge =
        "${imageHost}large/$id.png"
}