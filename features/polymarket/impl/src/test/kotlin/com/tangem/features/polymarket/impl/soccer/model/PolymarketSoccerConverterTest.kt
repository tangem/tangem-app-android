package com.tangem.features.polymarket.impl.soccer.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketSportsUM
import org.junit.jupiter.api.Test

internal class PolymarketSoccerConverterTest {
    @Test
    fun `GIVEN BFF display order opposed to home-away WHEN converted THEN teams and scores retain display order`() {
        // Arrange
        val event = soccerEvent(emptyList()).let { it.copy(teams = it.teams.reversed(), score = "3-1", period = "VFT") }

        // Act
        val actual = PolymarketSoccerConverter {}.convert(event, PolymarketSoccerSelection().reconcile(event))

        // Assert
        assertThat(actual.sports).isEqualTo(PolymarketSportsUM(
            home = PolymarketSportsUM.Team(stringReference("Away"), null),
            away = PolymarketSportsUM.Team(stringReference("Home"), null),
            homeScore = "3", awayScore = "1", period = "VFT", isLive = true,
        ))
    }

    @Test
    fun `GIVEN prematch with missing score WHEN converted THEN kickoff uses ET and scores stay absent`() {
        // Arrange
        val event = soccerEvent(emptyList()).copy(
            score = null, isLive = false, period = null, startTime = java.time.Instant.parse("2026-11-12T20:00:00Z"),
        )

        // Act
        val actual = PolymarketSoccerConverter {}.convert(event, PolymarketSoccerSelection().reconcile(event))

        // Assert
        assertThat(actual.kickoff).isEqualTo(stringReference("Nov 12, 2026, 3:00 PM ET"))
        assertThat(actual.sports).isEqualTo(PolymarketSportsUM(
            home = PolymarketSportsUM.Team(stringReference("Home"), null),
            away = PolymarketSportsUM.Team(stringReference("Away"), null),
            homeScore = null, awayScore = null, period = null, isLive = false,
        ))
    }
}