package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import java.math.BigDecimal

/**
 * Static fixtures backing [MockPolymarketRepository] while the feature is built UI-first.
 *
 * Covers both [PolymarketDisplayMode] variants and an event with a single market, since those drive the different
 * event card layouts.
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
            description = "This market will resolve to the team that wins the 2026 FIFA World Cup final.",
            volume = BigDecimal("48200000"),
            volume24h = BigDecimal("1250000"),
            liquidity = BigDecimal("3100000"),
            endDate = "2026-07-19T20:00:00Z",
            totalMarketsCount = 24,
            isNegRisk = true,
            displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
            markets = listOf(
                market(
                    id = "wc-2026-winner-argentina",
                    eventId = "fifa-wc-2026-winner",
                    title = "Will Argentina win the 2026 World Cup?",
                    slug = "will-argentina-win-the-2026-world-cup",
                    groupItemTitle = "Argentina",
                    volume = BigDecimal("9100000"),
                    volume24h = BigDecimal("310000"),
                    liquidity = BigDecimal("640000"),
                    endDate = "2026-07-19T20:00:00Z",
                    isNegRisk = true,
                    orderIndex = 0,
                    outcomes = listOf(
                        outcome(assetId = "wc-arg-yes", title = "Yes", probability = BigDecimal("0.18")),
                        outcome(assetId = "wc-arg-no", title = "No", probability = BigDecimal("0.82")),
                    ),
                ),
                market(
                    id = "wc-2026-winner-france",
                    eventId = "fifa-wc-2026-winner",
                    title = "Will France win the 2026 World Cup?",
                    slug = "will-france-win-the-2026-world-cup",
                    groupItemTitle = "France",
                    volume = BigDecimal("7600000"),
                    volume24h = BigDecimal("280000"),
                    liquidity = BigDecimal("520000"),
                    endDate = "2026-07-19T20:00:00Z",
                    isNegRisk = true,
                    orderIndex = 1,
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
            description = "This market will resolve to the club that wins the 2025/26 UEFA Champions League final.",
            volume = BigDecimal("21500000"),
            volume24h = BigDecimal("870000"),
            liquidity = BigDecimal("1400000"),
            endDate = "2026-05-30T19:00:00Z",
            totalMarketsCount = 16,
            isNegRisk = true,
            displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
            markets = listOf(
                market(
                    id = "ucl-2026-winner-real-madrid",
                    eventId = "ucl-2026-winner",
                    title = "Will Real Madrid win the 2025/26 Champions League?",
                    slug = "will-real-madrid-win-the-2025-26-champions-league",
                    groupItemTitle = "Real Madrid",
                    volume = BigDecimal("5300000"),
                    volume24h = BigDecimal("190000"),
                    liquidity = BigDecimal("410000"),
                    endDate = "2026-05-30T19:00:00Z",
                    isNegRisk = true,
                    orderIndex = 0,
                    outcomes = listOf(
                        outcome(assetId = "ucl-rma-yes", title = "Yes", probability = BigDecimal("0.22")),
                        outcome(assetId = "ucl-rma-no", title = "No", probability = BigDecimal("0.78")),
                    ),
                ),
                market(
                    id = "ucl-2026-winner-man-city",
                    eventId = "ucl-2026-winner",
                    title = "Will Manchester City win the 2025/26 Champions League?",
                    slug = "will-manchester-city-win-the-2025-26-champions-league",
                    groupItemTitle = "Manchester City",
                    volume = BigDecimal("4800000"),
                    volume24h = BigDecimal("170000"),
                    liquidity = BigDecimal("380000"),
                    endDate = "2026-05-30T19:00:00Z",
                    isNegRisk = true,
                    orderIndex = 1,
                    outcomes = listOf(
                        outcome(assetId = "ucl-mci-yes", title = "Yes", probability = BigDecimal("0.20")),
                        outcome(assetId = "ucl-mci-no", title = "No", probability = BigDecimal("0.80")),
                    ),
                ),
            ),
        ),
        // The BFF returns a single market when the event has only one active market.
        event(
            id = "ballon-dor-2026",
            slug = "ballon-dor-2026-winner",
            title = "Ballon d'Or 2026 Winner",
            description = "This market will resolve to the player awarded the 2026 Ballon d'Or.",
            volume = BigDecimal("6400000"),
            volume24h = BigDecimal("210000"),
            liquidity = BigDecimal("480000"),
            endDate = "2026-10-26T18:30:00Z",
            totalMarketsCount = 8,
            isNegRisk = true,
            displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
            markets = listOf(
                market(
                    id = "ballon-dor-2026-mbappe",
                    eventId = "ballon-dor-2026",
                    title = "Will Kylian Mbappé win the 2026 Ballon d'Or?",
                    slug = "will-kylian-mbappe-win-the-2026-ballon-dor",
                    groupItemTitle = "Kylian Mbappé",
                    volume = BigDecimal("1900000"),
                    volume24h = BigDecimal("64000"),
                    liquidity = BigDecimal("150000"),
                    endDate = "2026-10-26T18:30:00Z",
                    isNegRisk = true,
                    orderIndex = 0,
                    outcomes = listOf(
                        outcome(assetId = "bd-mbappe-yes", title = "Yes", probability = BigDecimal("0.31")),
                        outcome(assetId = "bd-mbappe-no", title = "No", probability = BigDecimal("0.69")),
                    ),
                ),
            ),
        ),
        // Independent markets, so the card renders every market with its own outcomes.
        event(
            id = "football-transfers-2026",
            slug = "football-transfers-summer-2026",
            title = "Football Transfers: Summer 2026",
            description = "Each market resolves independently once the summer 2026 transfer window closes.",
            volume = BigDecimal("3800000"),
            volume24h = BigDecimal("145000"),
            liquidity = BigDecimal("260000"),
            endDate = "2026-09-01T21:00:00Z",
            totalMarketsCount = 6,
            isNegRisk = false,
            displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
            markets = listOf(
                market(
                    id = "transfers-2026-haaland-madrid",
                    eventId = "football-transfers-2026",
                    title = "Will Erling Haaland join Real Madrid before September 2026?",
                    slug = "will-erling-haaland-join-real-madrid-before-september-2026",
                    volume = BigDecimal("1100000"),
                    volume24h = BigDecimal("48000"),
                    liquidity = BigDecimal("92000"),
                    endDate = "2026-09-01T21:00:00Z",
                    orderIndex = 0,
                    outcomes = listOf(
                        outcome(assetId = "tr-haaland-yes", title = "Yes", probability = BigDecimal("0.12")),
                        outcome(assetId = "tr-haaland-no", title = "No", probability = BigDecimal("0.88")),
                    ),
                ),
                market(
                    id = "transfers-2026-vinicius-stays",
                    eventId = "football-transfers-2026",
                    title = "Will Vinícius Júnior stay at Real Madrid past September 2026?",
                    slug = "will-vinicius-junior-stay-at-real-madrid-past-september-2026",
                    volume = BigDecimal("940000"),
                    volume24h = BigDecimal("37000"),
                    liquidity = BigDecimal("75000"),
                    endDate = "2026-09-01T21:00:00Z",
                    orderIndex = 1,
                    outcomes = listOf(
                        outcome(assetId = "tr-vini-yes", title = "Yes", probability = BigDecimal("0.74")),
                        outcome(assetId = "tr-vini-no", title = "No", probability = BigDecimal("0.26")),
                    ),
                ),
            ),
        ),
    )

    private fun event(
        id: String,
        slug: String,
        title: String,
        description: String = "",
        rulesUrl: String = "https://polymarket.com/rules",
        iconUrl: String? = null,
        imageUrl: String? = null,
        status: PolymarketStatus = PolymarketStatus.ACTIVE,
        startDate: String? = null,
        endDate: String? = null,
        volume: BigDecimal? = null,
        volume24h: BigDecimal? = null,
        liquidity: BigDecimal? = null,
        totalMarketsCount: Int = 0,
        isNegRisk: Boolean = false,
        displayMode: PolymarketDisplayMode = PolymarketDisplayMode.PLAIN_MARKETS,
        markets: List<PolymarketMarket> = emptyList(),
    ): PolymarketEvent = PolymarketEvent(
        id = id,
        slug = slug,
        title = title,
        description = description,
        rulesUrl = rulesUrl,
        iconUrl = iconUrl,
        imageUrl = imageUrl,
        status = status,
        startDate = startDate,
        endDate = endDate,
        volume = volume,
        volume24h = volume24h,
        liquidity = liquidity,
        totalMarketsCount = totalMarketsCount,
        isNegRisk = isNegRisk,
        displayMode = displayMode,
        markets = markets,
    )

    private fun market(
        id: String,
        eventId: String,
        title: String,
        slug: String,
        description: String = "",
        groupItemTitle: String? = null,
        iconUrl: String? = null,
        imageUrl: String? = null,
        status: PolymarketStatus = PolymarketStatus.ACTIVE,
        isNegRisk: Boolean = false,
        startDate: String? = null,
        endDate: String? = null,
        startDateIso: String? = null,
        endDateIso: String? = null,
        volume: BigDecimal? = null,
        volume24h: BigDecimal? = null,
        liquidity: BigDecimal? = null,
        orderIndex: Int = 0,
        outcomes: List<PolymarketOutcome> = emptyList(),
    ): PolymarketMarket = PolymarketMarket(
        id = id,
        eventId = eventId,
        title = title,
        slug = slug,
        description = description,
        groupItemTitle = groupItemTitle,
        iconUrl = iconUrl,
        imageUrl = imageUrl,
        status = status,
        isNegRisk = isNegRisk,
        startDate = startDate,
        endDate = endDate,
        startDateIso = startDateIso,
        endDateIso = endDateIso,
        volume = volume,
        volume24h = volume24h,
        liquidity = liquidity,
        orderIndex = orderIndex,
        outcomes = outcomes,
    )

    private fun outcome(assetId: String, title: String, probability: BigDecimal? = null): PolymarketOutcome =
        PolymarketOutcome(
            assetId = assetId,
            title = title,
            probability = probability,
        )
}