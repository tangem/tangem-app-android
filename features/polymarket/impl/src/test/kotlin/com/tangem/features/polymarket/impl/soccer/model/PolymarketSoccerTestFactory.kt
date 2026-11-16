package com.tangem.features.polymarket.impl.soccer.model

import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import java.math.BigDecimal

internal fun soccerEvent(cards: List<PolymarketSoccerCard>): PolymarketSoccerEvent = PolymarketSoccerEvent(
    id = "main", slug = "match", title = "Home vs Away", description = "Match description", rulesUrl = "https://example.com",
    imageUrl = null, iconUrl = "match.png", status = PolymarketStatus.ACTIVE, isLive = true, startTime = null,
    startDate = null, endDate = null, volume = null, volume24h = null, liquidity = null, isNegRisk = true,
    gameId = "game", score = "1-0", period = "1H", elapsed = "30", isEnded = false, dataFetchedAt = null,
    league = PolymarketSoccerEvent.League("epl", "Premier League", null), resolutionSource = null,
    sportOrdering = "home", teams = listOf(
        PolymarketTeam("Home", "HOM", null, null, "home"), PolymarketTeam("Away", "AWY", null, null, "away"),
    ), tabs = listOf(PolymarketSoccerTab(PolymarketSoccerTab.Key.GAME_LINES, cards)),
)

internal fun threeWayCard(key: PolymarketSoccerCard.Key = PolymarketSoccerCard.Key.MONEYLINE): PolymarketSoccerCard {
    val sides = listOf(PolymarketSoccerCard.ButtonSide.LEFT, PolymarketSoccerCard.ButtonSide.MIDDLE,
        PolymarketSoccerCard.ButtonSide.RIGHT)
    val labels = listOf("HOM", "Provider middle label", "AWY")
    return PolymarketSoccerCard(
        key = key, sportsMarketType = "moneyline",
        markets = sides.mapIndexed { index, side ->
            createPolymarketMarket(
                id = side.value, eventId = "child-${side.value}", description = "Rules ${side.value}",
                isNegRisk = index != 1, volume = BigDecimal.TEN, outcomes = listOf(
                    createPolymarketOutcome("${side.value}-no", "No", BigDecimal("0.99")),
                    createPolymarketOutcome("${side.value}-yes", "Yes", if (index == 1) null else BigDecimal("0.42")),
                ),
            )
        },
        content = PolymarketSoccerCard.Content.ThreeWay(sides.mapIndexed { index, side ->
            PolymarketSoccerCard.Button(side, labels[index], side.value, "${side.value}-yes")
        }),
    )
}

internal fun soccerPickerCard(
    key: PolymarketSoccerCard.Key = PolymarketSoccerCard.Key.SPREADS,
    teamSide: PolymarketSoccerCard.TeamSide? = null,
    defaultMarketId: String = "p2",
): PolymarketSoccerCard {
    val spreads = key == PolymarketSoccerCard.Key.SPREADS
    val positions = listOf("p1", "p2").mapIndexed { index, id ->
        val line = if (spreads) (if (index == 0) "1.5" else "-1.5") else (if (index == 0) "1.5" else "2.5")
        val left = if (spreads) PolymarketSoccerCard.ButtonSide.LEFT else PolymarketSoccerCard.ButtonSide.OVER
        val right = if (spreads) PolymarketSoccerCard.ButtonSide.RIGHT else PolymarketSoccerCard.ButtonSide.UNDER
        PolymarketSoccerCard.Position(
            marketId = id,
            pickerLabel = if (spreads) "1.5" else line,
            line = BigDecimal(line),
            isDefault = id == defaultMarketId,
            buttons = listOf(
                PolymarketSoccerCard.Button(left, if (spreads) "HOM ${if (index == 0) "+" else ""}$line" else "provider over", id, "$id-left"),
                PolymarketSoccerCard.Button(right, if (spreads) "AWY ${BigDecimal(line).negate()}" else "provider under", id, "$id-right"),
            ),
        )
    }
    return PolymarketSoccerCard(
        key = key,
        sportsMarketType = key.value,
        markets = positions.map { position ->
            createPolymarketMarket(
                id = position.marketId,
                eventId = "child-${position.marketId}",
                description = "Rules ${position.marketId}",
                outcomes = listOf(
                    createPolymarketOutcome("${position.marketId}-right", probability = BigDecimal("0.7")),
                    createPolymarketOutcome("${position.marketId}-left", probability = null),
                ),
            )
        },
        content = if (spreads) PolymarketSoccerCard.Content.SidePicker(positions)
        else PolymarketSoccerCard.Content.OverUnderPicker(positions, teamSide),
    )
}