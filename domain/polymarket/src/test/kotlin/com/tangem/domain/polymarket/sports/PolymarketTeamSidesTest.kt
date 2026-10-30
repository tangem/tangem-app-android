package com.tangem.domain.polymarket.sports

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSportsState.Team
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketTeamSidesTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN team metadata WHEN resolving sides THEN ordering wins and price association is preserved`(model: Model) {
        // Arrange
        val teams = model.teams

        // Act
        val actual = PolymarketTeamSides.resolve(teams, model.home, model.away)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<Model> {
        val home = createSportsTeam("Alpha", "home")
        val away = createSportsTeam("Beta", "away")
        val first = home.copy(ordering = null)
        val second = away.copy(ordering = null)
        return listOf(
            Model(listOf(away, home), "Beta", "Alpha", Team(home, 1) to Team(away, 0)),
            Model(listOf(second, first), " alpha ", "BETA", Team(first, 1) to Team(second, 0)),
            Model(listOf(second, first), null, null, Team(second, 0) to Team(first, 1)),
            Model(listOf(away, first), null, null, Team(first, 1) to Team(away, 0)),
            Model(listOf(second, first), null, "Beta", Team(first, 1) to Team(second, 0)),
            Model(listOf(first), null, null, Team(first, 0) to null),
            Model(emptyList(), "Alpha", "Beta", Team(PolymarketTeam("Alpha", null, null, null, "home"), null) to
                Team(PolymarketTeam("Beta", null, null, null, "away"), null)),
            Model(emptyList(), null, null, null to null),
        )
    }

    data class Model(
        val teams: List<PolymarketTeam>,
        val home: String?,
        val away: String?,
        val expected: Pair<Team?, Team?>,
    )
}