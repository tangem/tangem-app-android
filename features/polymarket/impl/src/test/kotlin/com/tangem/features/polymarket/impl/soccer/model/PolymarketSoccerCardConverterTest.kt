package com.tangem.features.polymarket.impl.soccer.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.formatPolymarketVolume
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class PolymarketSoccerCardConverterTest {
    @BeforeEach
    fun setUp() {
        mockkStatic(BigDecimal?::formatPolymarketVolume)
        every { BigDecimal("30").formatPolymarketVolume() } returns "$30"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(BigDecimal?::formatPolymarketVolume)
    }

    @Test
    fun `GIVEN three-way markets WHEN converted THEN buttons target their owning events and exact asset prices`() {
        // Arrange
        val card = threeWayCard()
        val title = resourceReference(R.string.prediction_match_card_moneyline)
        val expected = PolymarketSoccerCardUM(
            id = "game_lines/moneyline", title = title, iconUrl = "match.png", volume = stringReference("$30"),
            rules = PolymarketSoccerCardUM.Rules(title, "Rules left"),
            content = PolymarketSoccerCardUM.Content.Buttons(persistentListOf(
                PolymarketSoccerCardUM.Outcome(
                    PolymarketSoccerCardUM.Target("child-left", "left", "left-yes"), stringReference("HOM"),
                    stringReference("42¢"), PolymarketOutcomeAccent.Info,
                ),
                PolymarketSoccerCardUM.Outcome(
                    PolymarketSoccerCardUM.Target("child-middle", "middle", "middle-yes"),
                    resourceReference(R.string.prediction_sports_draw), stringReference("—"),
                    PolymarketOutcomeAccent.Neutral,
                ),
                PolymarketSoccerCardUM.Outcome(
                    PolymarketSoccerCardUM.Target("child-right", "right", "right-yes"), stringReference("AWY"),
                    stringReference("42¢"), PolymarketOutcomeAccent.Error,
                ),
            )),
        )

        // Act
        val result = PolymarketSoccerCardConverter.convert(soccerEvent(listOf(card)), PolymarketSoccerTab.Key.GAME_LINES, card)

        // Assert
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN first-to-score WHEN converted THEN middle label means neither and is not parsed from provider text`() {
        // Arrange
        val card = threeWayCard(PolymarketSoccerCard.Key.FIRST_TO_SCORE)

        // Act
        val result = requireNotNull(PolymarketSoccerCardConverter.convert(
            soccerEvent(listOf(card)), PolymarketSoccerTab.Key.FIRST_HALF, card,
        ))

        // Assert
        assertThat((result.content as PolymarketSoccerCardUM.Content.Buttons).outcomes[1].title)
            .isEqualTo(resourceReference(R.string.prediction_sports_neither))
        assertThat(result.id).isEqualTo("first_half/first_to_score")
    }

    @Test
    fun `GIVEN half-time result WHEN converted THEN title is result and middle outcome is draw`() {
        // Arrange
        val card = threeWayCard(PolymarketSoccerCard.Key.RESULT)

        // Act
        val result = requireNotNull(PolymarketSoccerCardConverter.convert(
            soccerEvent(listOf(card)), PolymarketSoccerTab.Key.FIRST_HALF, card,
        ))

        // Assert
        assertThat(result.title).isEqualTo(resourceReference(R.string.prediction_match_card_result))
        assertThat((result.content as PolymarketSoccerCardUM.Content.Buttons).outcomes[1].title)
            .isEqualTo(resourceReference(R.string.prediction_sports_draw))
    }

    @Test
    fun `GIVEN exact scores WHEN converted THEN row labels and BFF order are retained`() {
        // Arrange
        val base = threeWayCard()
        val card = base.copy(key = PolymarketSoccerCard.Key.EXACT_SCORE,
            content = PolymarketSoccerCard.Content.Rows(listOf(
                PolymarketSoccerCard.Button(PolymarketSoccerCard.ButtonSide.YES, "2-1", "right", "right-yes"),
                PolymarketSoccerCard.Button(PolymarketSoccerCard.ButtonSide.YES, "Any Other Score", "left", "left-yes"),
            )))

        // Act
        val result = requireNotNull(PolymarketSoccerCardConverter.convert(
            soccerEvent(listOf(card)), PolymarketSoccerTab.Key.EXACT_SCORE, card,
        ))

        // Assert
        assertThat((result.content as PolymarketSoccerCardUM.Content.Rows).outcomes.map { it.title })
            .containsExactly(stringReference("2-1"), stringReference("Any Other Score")).inOrder()
        assertThat(result.rules?.description).isEqualTo("Rules left")
    }

    @Test
    fun `GIVEN yes-no with empty rules WHEN converted THEN labels are localized and rules action is absent`() {
        // Arrange
        val market = createPolymarketMarket(id = "btts", description = " ", outcomes = listOf(
            createPolymarketOutcome("yes"), createPolymarketOutcome("no"),
        ))
        val card = PolymarketSoccerCard(
            PolymarketSoccerCard.Key.BOTH_TEAMS_TO_SCORE, null, listOf(market),
            PolymarketSoccerCard.Content.YesNo(listOf(
                PolymarketSoccerCard.Button(PolymarketSoccerCard.ButtonSide.YES, "provider", "btts", "yes"),
                PolymarketSoccerCard.Button(PolymarketSoccerCard.ButtonSide.NO, "provider", "btts", "no"),
            )),
        )

        // Act
        val result = requireNotNull(PolymarketSoccerCardConverter.convert(
            soccerEvent(listOf(card)), PolymarketSoccerTab.Key.GAME_LINES, card,
        ))

        // Assert
        assertThat(result.rules).isNull()
        assertThat((result.content as PolymarketSoccerCardUM.Content.Buttons).outcomes.map { it.title })
            .containsExactly(resourceReference(R.string.common_yes), resourceReference(R.string.common_no)).inOrder()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN dangling reference WHEN converted THEN partial card is not displayed`(missingMarket: Boolean) {
        // Arrange
        val base = threeWayCard()
        val card = if (missingMarket) base.copy(markets = base.markets.drop(1))
        else base.copy(markets = base.markets.map { it.copy(outcomes = emptyList()) })

        // Act
        val result = PolymarketSoccerCardConverter.convert(soccerEvent(listOf(card)), PolymarketSoccerTab.Key.GAME_LINES, card)

        // Assert
        assertThat(result).isNull()
    }
}