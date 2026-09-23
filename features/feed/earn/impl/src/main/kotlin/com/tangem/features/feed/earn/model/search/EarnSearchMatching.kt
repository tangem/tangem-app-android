package com.tangem.features.feed.earn.model.search

import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import java.math.BigDecimal

/**
 * Filters the earn catalogue locally, because the earn endpoint takes no query parameter.
 *
 * Ranking is ours for the same reason — the backend orders the catalogue by yield and knows nothing
 * about the query, so a plain `contains` would bury the token the user typed under higher-yield
 * look-alikes. Ties fall back to the yield order the Featured carousel already uses.
 *
 * A token offering both staking and yield stays as two results: they are two different opportunities,
 * and their row ids differ by earn type.
 */
internal fun List<EarnTokenWithCurrency>.matching(query: String): List<EarnTokenWithCurrency> {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return emptyList()

    return mapNotNull { item ->
        val rank = item.rankFor(normalized) ?: return@mapNotNull null
        rank to item
    }
        .sortedWith(
            compareBy<Pair<Int, EarnTokenWithCurrency>> { (rank, _) -> rank }
                .thenByDescending { (_, item) -> item.apyOrZero() }
                .thenBy { (_, item) -> item.earnToken.tokenName },
        )
        .map { (_, item) -> item }
}

@Suppress("MagicNumber")
private fun EarnTokenWithCurrency.rankFor(query: String): Int? {
    val symbol = earnToken.tokenSymbol.lowercase()
    val name = earnToken.tokenName.lowercase()

    return when {
        symbol == query -> 0
        symbol.startsWith(query) -> 1
        name.startsWith(query) -> 2
        symbol.contains(query) || name.contains(query) -> 3
        else -> null
    }
}

private fun EarnTokenWithCurrency.apyOrZero(): BigDecimal = earnToken.apy.parseBigDecimalOrNull() ?: BigDecimal.ZERO