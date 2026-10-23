package com.tangem.features.polymarket.impl

import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import java.math.BigDecimal

internal fun createPolymarketEvent(
    id: String = "event-1",
    slug: String = "event-slug",
    title: String = "Event title",
    description: String = "Event description",
    rulesUrl: String = "https://polymarket.com/rules",
    iconUrl: String? = null,
    imageUrl: String? = null,
    status: PolymarketStatus = PolymarketStatus.ACTIVE,
    startDate: String? = null,
    endDate: String? = null,
    volume: BigDecimal? = null,
    volume24h: BigDecimal? = null,
    liquidity: BigDecimal? = null,
    isNegRisk: Boolean = false,
    isLive: Boolean = false,
    displayMode: PolymarketDisplayMode = PolymarketDisplayMode.PLAIN_MARKETS,
    markets: List<PolymarketMarket> = listOf(createPolymarketMarket(eventId = id)),
    totalMarketsCount: Int = markets.size,
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
    isLive = isLive,
    displayMode = displayMode,
    markets = markets,
)

internal fun createPolymarketMarket(
    id: String = "market-1",
    conditionId: String? = null,
    eventId: String = "event-1",
    title: String = "Market question",
    slug: String = "market-slug",
    description: String = "Market description",
    groupItemTitle: String? = null,
    iconUrl: String? = null,
    imageUrl: String? = null,
    status: PolymarketStatus = PolymarketStatus.ACTIVE,
    isNegRisk: Boolean = false,
    isAcceptingOrders: Boolean = true,
    startDate: String? = null,
    endDate: String? = null,
    startDateIso: String? = null,
    endDateIso: String? = null,
    volume: BigDecimal? = null,
    volume24h: BigDecimal? = null,
    liquidity: BigDecimal? = null,
    orderIndex: Int = 0,
    outcomes: List<PolymarketOutcome> = listOf(createPolymarketOutcome()),
    version: PolymarketMarketVersion = PolymarketMarketVersion.V1,
): PolymarketMarket = PolymarketMarket(
    id = id,
    conditionId = conditionId,
    eventId = eventId,
    title = title,
    slug = slug,
    description = description,
    groupItemTitle = groupItemTitle,
    iconUrl = iconUrl,
    imageUrl = imageUrl,
    status = status,
    isNegRisk = isNegRisk,
    isAcceptingOrders = isAcceptingOrders,
    startDate = startDate,
    endDate = endDate,
    startDateIso = startDateIso,
    endDateIso = endDateIso,
    volume = volume,
    volume24h = volume24h,
    liquidity = liquidity,
    orderIndex = orderIndex,
    outcomes = outcomes,
    version = version,
)

internal fun createPolymarketOutcome(
    assetId: String = "asset-1",
    title: String = "Yes",
    probability: BigDecimal? = null,
    legacyAssetId: String? = null,
): PolymarketOutcome = PolymarketOutcome(
    assetId = assetId,
    title = title,
    probability = probability,
    legacyAssetId = legacyAssetId,
)