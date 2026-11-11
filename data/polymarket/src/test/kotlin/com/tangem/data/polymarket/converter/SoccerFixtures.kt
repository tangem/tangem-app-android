package com.tangem.data.polymarket.converter

import com.tangem.grow.datasource.polymarket.models.PolymarketMarketDto
import com.tangem.grow.datasource.polymarket.models.PolymarketOutcomeDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerButtonDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerCardDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerEventDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerLeagueDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerPickerPositionDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerTabDto
import com.tangem.grow.datasource.polymarket.models.PolymarketTeamDto

internal object SoccerFixtures {
    fun market(id: String = "market") = PolymarketMarketDto(
        id = id, conditionId = "condition-$id", eventId = "child", question = "Question", slug = id,
        description = "Rules $id", groupItemTitle = null, image = null, icon = null, status = "active",
        isNegRisk = false, isAcceptingOrders = true, startDate = null, endDate = null, startDateIso = null,
        endDateIso = null, volume = null, volume24hr = null, liquidity = null, orderIndex = 0, version = "v2",
        sportsMarketType = "spreads", line = -1.5,
        outcomes = listOf(
            PolymarketOutcomeDto("$id-yes", "Yes", 0.42),
            PolymarketOutcomeDto("$id-no", "No", null),
        ),
    )

    fun button(side: String, market: String = "market", outcome: String = "yes") = PolymarketSoccerButtonDto(
        side = side, label = "Label $side", marketId = market, assetId = "$market-$outcome",
    )

    fun position(id: String, line: Double, isDefault: Boolean) = PolymarketSoccerPickerPositionDto(
        pickerLabel = "1.5", line = line, isDefault = isDefault, marketId = id,
        buttons = listOf(button("left", id), button("right", id, "no")),
    )

    fun spreads() = PolymarketSoccerCardDto(
        key = "spreads", template = "side_picker", sportsMarketType = "spreads",
        markets = listOf(market("plus"), market("minus").copy(isNegRisk = true)),
        positions = listOf(position("plus", 1.5, false), position("minus", -1.5, true)),
    )

    fun event(cards: List<PolymarketSoccerCardDto> = listOf(spreads())) = PolymarketSoccerEventDto(
        eventId = "main", slug = "main-slug", title = "Match", description = "Event rules",
        polymarketRulesUrl = "https://polymarket.com", image = null, icon = null, status = "active", isLive = false,
        startTime = "2026-11-12T20:00:00Z", startDate = "2026-10-01T00:00:00Z", endDate = null,
        volume = 123.5, volume24hr = null, liquidity = null, isNegRisk = true,
        gameId = "game", score = "0-0", period = "NS", elapsed = "", isEnded = false,
        dataFetchedAt = "2026-11-12T19:00:00Z", league = PolymarketSoccerLeagueDto("epl", "Premier League", null),
        resolutionSource = "source", sportOrdering = "away",
        teams = listOf(
            PolymarketTeamDto("Away", "AWY", null, null, "away"),
            PolymarketTeamDto("Home", "HOM", null, null, "home"),
        ),
        tabs = listOf(PolymarketSoccerTabDto("game_lines", cards)),
    )
}