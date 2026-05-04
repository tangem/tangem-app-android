package com.tangem.domain.search.model

import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

/**
 * @property timestamp epoch milliseconds
 * @property price fiat quote in the app currency that was active when the token was saved
 */
data class RecentSearchToken(
    val id: CryptoCurrency.RawID,
    val name: String,
    val symbol: String,
    val imageUrl: String?,
    val timestamp: Long,
    val appCurrencyCode: String,
    val appCurrencySymbol: String,
    val price: BigDecimal,
    val priceChangePercent: String,
    val priceChangeDirection: TokenPriceChangeDirection,
    val marketCap: String?,
    val ratingPosition: String?,
    val isUnder100kMarketCap: Boolean,
    val stakingRateText: String?,
)