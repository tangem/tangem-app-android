package com.tangem.features.polymarket.impl.soccer.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import org.junit.jupiter.api.Test

internal class PolymarketSoccerPricePlanTest {
    @Test
    fun `GIVEN all templates across tabs WHEN planning THEN poll every button and picker position once`() {
        // Arrange
        val base = threeWayCard()
        val buttons = (base.content as PolymarketSoccerCard.Content.ThreeWay).buttons
        val rows = base.copy(content = PolymarketSoccerCard.Content.Rows(buttons))
        val yesNo = base.copy(content = PolymarketSoccerCard.Content.YesNo(buttons.take(2)))
        val event = soccerEvent(listOf(base, soccerPickerCard())).copy(isLive = false).let {
            it.copy(tabs = it.tabs + listOf(
                PolymarketSoccerTab(PolymarketSoccerTab.Key.EXACT_SCORE, listOf(rows)),
                PolymarketSoccerTab(PolymarketSoccerTab.Key.FIRST_HALF, listOf(yesNo)),
                PolymarketSoccerTab(PolymarketSoccerTab.Key.SECOND_HALF, listOf(soccerPickerCard(PolymarketSoccerCard.Key.TOTALS))),
            ))
        }

        // Act
        val plan = PolymarketSoccerPricePlan.create(event)

        // Assert
        assertThat(plan).isEqualTo(PolymarketSoccerPricePlan(
            "main", false,
            setOf("left-yes", "middle-yes", "right-yes", "p1-left", "p1-right", "p2-left", "p2-right"),
            emptySet(),
        ))
    }

    @Test
    fun `GIVEN moneyline only in first half WHEN live THEN keep it in polling`() {
        // Arrange
        val event = soccerEvent(emptyList()).copy(tabs = listOf(
            PolymarketSoccerTab(PolymarketSoccerTab.Key.FIRST_HALF, listOf(threeWayCard())),
        ))

        // Act
        val plan = PolymarketSoccerPricePlan.create(event)

        // Assert
        assertThat(plan).isEqualTo(PolymarketSoccerPricePlan(
            "main", false, setOf("left-yes", "middle-yes", "right-yes"), emptySet(),
        ))
    }

    @Test
    fun `GIVEN missing market and outcome references WHEN planning THEN ignore dangling button assets`() {
        // Arrange
        val card = threeWayCard().let { card -> card.copy(markets = card.markets.drop(1).map { market ->
            if (market.id == "middle") market.copy(outcomes = emptyList()) else market
        }) }
        val event = soccerEvent(listOf(card)).copy(isLive = false)

        // Act
        val plan = PolymarketSoccerPricePlan.create(event)

        // Assert
        assertThat(plan.polled).containsExactly("right-yes")
        assertThat(plan.streamed).isEmpty()
    }

    @Test
    fun `GIVEN ended with inconsistent live flag WHEN planning THEN neither source is used`() {
        // Arrange
        val event = soccerEvent(listOf(threeWayCard(), soccerPickerCard())).copy(isEnded = true)

        // Act
        val plan = PolymarketSoccerPricePlan.create(event)

        // Assert
        assertThat(plan).isEqualTo(PolymarketSoccerPricePlan("main", true, emptySet(), emptySet()))
    }
}