package com.tangem.domain.search.model

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * @property timestamp epoch milliseconds
 */
data class RecentSearchToken(
    val id: CryptoCurrency.RawID,
    val name: String,
    val symbol: String,
    val imageUrl: String?,
    val timestamp: Long,
)