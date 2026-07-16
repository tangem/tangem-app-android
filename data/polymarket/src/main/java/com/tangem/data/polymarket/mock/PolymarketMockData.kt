package com.tangem.data.polymarket.mock

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import java.math.BigDecimal

/**
 * Static fixtures backing [MockPolymarketRepository] while the feature is built UI-first.
 *
 * Factory helpers default every field so a call site overrides only what it needs and new model fields don't
 * churn existing fixtures.
 */
internal object PolymarketMockData {

    val events: List<PolymarketEvent> = listOf(
        event(
            id = "fifa-wc-2026-winner",
            slug = "world-cup-2026-winner",
            title = "FIFA World Cup 2026 Winner",
            volume = BigDecimal("48200000"),
            totalMarketsCount = 24,
            markets = listOf(
                market(
                    id = "wc-2026-winner-argentina",
                    title = "Will Argentina win the 2026 World Cup?",
                    volume = BigDecimal("9100000"),
                    outcomes = listOf(
                        outcome(assetId = "wc-arg-yes", title = "Yes", probability = BigDecimal("0.18")),
                        outcome(assetId = "wc-arg-no", title = "No", probability = BigDecimal("0.82")),
                    ),
                ),
                market(
                    id = "wc-2026-winner-france",
                    title = "Will France win the 2026 World Cup?",
                    volume = BigDecimal("7600000"),
                    outcomes = listOf(
                        outcome(assetId = "wc-fra-yes", title = "Yes", probability = BigDecimal("0.15")),
                        outcome(assetId = "wc-fra-no", title = "No", probability = BigDecimal("0.85")),
                    ),
                ),
            ),
        ),
        event(
            id = "ucl-2026-winner",
            slug = "champions-league-2025-26-winner",
            title = "UEFA Champions League 2025/26 Winner",
            volume = BigDecimal("21500000"),
            totalMarketsCount = 16,
            markets = listOf(
                market(
                    id = "ucl-2026-winner-real-madrid",
                    title = "Will Real Madrid win the 2025/26 Champions League?",
                    volume = BigDecimal("5300000"),
                    outcomes = listOf(
                        outcome(assetId = "ucl-rma-yes", title = "Yes", probability = BigDecimal("0.22")),
                        outcome(assetId = "ucl-rma-no", title = "No", probability = BigDecimal("0.78")),
                    ),
                ),
                market(
                    id = "ucl-2026-winner-man-city",
                    title = "Will Manchester City win the 2025/26 Champions League?",
                    volume = BigDecimal("4800000"),
                    outcomes = listOf(
                        outcome(assetId = "ucl-mci-yes", title = "Yes", probability = BigDecimal("0.20")),
                        outcome(assetId = "ucl-mci-no", title = "No", probability = BigDecimal("0.80")),
                    ),
                ),
            ),
        ),
        event(
            id = "ballon-dor-2026",
            slug = "ballon-dor-2026-winner",
            title = "Ballon d'Or 2026 Winner",
            volume = BigDecimal("6400000"),
            totalMarketsCount = 8,
            markets = listOf(
                market(
                    id = "ballon-dor-2026-mbappe",
                    title = "Will Kylian Mbappé win the 2026 Ballon d'Or?",
                    volume = BigDecimal("1900000"),
                    outcomes = listOf(
                        outcome(assetId = "bd-mbappe-yes", title = "Yes", probability = BigDecimal("0.31")),
                        outcome(assetId = "bd-mbappe-no", title = "No", probability = BigDecimal("0.69")),
                    ),
                ),
            ),
        ),
    )

    private fun event(
        id: String,
        slug: String,
        title: String,
        iconUrl: String? = null,
        imageUrl: String? = null,
        volume: BigDecimal? = null,
        totalMarketsCount: Int = 0,
        markets: List<PolymarketMarket> = emptyList(),
    ): PolymarketEvent = PolymarketEvent(
        id = id,
        slug = slug,
        title = title,
        iconUrl = iconUrl,
        imageUrl = imageUrl,
        volume = volume,
        totalMarketsCount = totalMarketsCount,
        markets = markets,
    )

    private fun market(
        id: String,
        title: String,
        iconUrl: String? = null,
        volume: BigDecimal? = null,
        outcomes: List<PolymarketOutcome> = emptyList(),
    ): PolymarketMarket = PolymarketMarket(
        id = id,
        title = title,
        iconUrl = iconUrl,
        volume = volume,
        outcomes = outcomes,
    )

    private fun outcome(assetId: String, title: String, probability: BigDecimal? = null): PolymarketOutcome =
        PolymarketOutcome(
            assetId = assetId,
            title = title,
            probability = probability,
        )
}