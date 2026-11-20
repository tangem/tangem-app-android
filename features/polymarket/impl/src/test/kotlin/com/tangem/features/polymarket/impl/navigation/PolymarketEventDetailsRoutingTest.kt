package com.tangem.features.polymarket.impl.navigation

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createSoccerMatchTeams
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketEventDetailsRoutingTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN event type and teams WHEN soccer match checked THEN only a soccer event with two teams is one`(
        model: SoccerMatchModel,
    ) {
        // Arrange
        val event = createPolymarketEvent().copy(eventType = model.eventType, teams = model.teams)

        // Act
        val actual = event.isSoccerMatch

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        SoccerMatchModel(eventType = "soccer", teams = createSoccerMatchTeams(), expected = true),
        SoccerMatchModel(eventType = "soccer", teams = null, expected = false),
        SoccerMatchModel(eventType = "soccer", teams = emptyList(), expected = false),
        SoccerMatchModel(eventType = "soccer", teams = createSoccerMatchTeams().take(1), expected = false),
        SoccerMatchModel(eventType = "basketball", teams = createSoccerMatchTeams(), expected = false),
        SoccerMatchModel(eventType = null, teams = createSoccerMatchTeams(), expected = false),
    )

    data class SoccerMatchModel(val eventType: String?, val teams: List<PolymarketTeam>?, val expected: Boolean)
}