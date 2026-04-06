package com.tangem.data.search.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SearchHistoryDTO(
    val textHints: List<TextHintDTO> = emptyList(),
    val recentTokens: List<RecentTokenDTO> = emptyList(),
)

@JsonClass(generateAdapter = true)
internal data class TextHintDTO(
    val text: String,
    val timestamp: Long,
)

/**
 * @param appCurrencyCode currency code (e.g. "USD") the [pricePlain] was quoted in
 */
@JsonClass(generateAdapter = true)
internal data class RecentTokenDTO(
    val id: String,
    val name: String,
    val symbol: String,
    val imageUrl: String?,
    val timestamp: Long,
    val pricePlain: String,
    val appCurrencyCode: String,
    val appCurrencySymbol: String,
    val priceChangePercent: String,
    val priceChangeDirection: String,
    val marketCap: String?,
    val ratingPosition: String?,
    val isUnder100kMarketCap: Boolean,
    val stakingRateText: String?,
)