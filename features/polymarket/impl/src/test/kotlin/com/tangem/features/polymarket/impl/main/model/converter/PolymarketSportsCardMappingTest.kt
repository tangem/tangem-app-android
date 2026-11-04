package com.tangem.features.polymarket.impl.main.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketEventUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSportsCardMappingTest {

    private val noAction: () -> Unit = {}
    private val home = PolymarketTeam("Home", "HME", null, null, "home")
    private val away = PolymarketTeam("Away", "AWY", null, null, "away")

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN lifecycle and market coverage WHEN mapping THEN only complete active moneylines use sports cards`(
        model: Model,
    ) {
        // Arrange
        val clicks = mutableListOf<Triple<String, String, String>>()
        val converter = PolymarketEventUMConverter(
            onEventClick = {},
            onOutcomeClick = { event, market, asset -> clicks += Triple(event, market, asset) },
            isPhaseTwoEnabled = model.isEnabled,
        )
        val regular = converter.convert(model.event)

        // Act
        val actual = converter.convert(model.event, model.game)
        actual.rows.flatMap { it.outcomes }.forEach { it.onClick() }

        // Assert
        assertThat(actual.hiddenMarketsCount).isEqualTo(regular.hiddenMarketsCount)
        if (model.shouldShowSports) {
            assertThat(actual.sports?.home?.name).isEqualTo(stringReference("Home"))
            assertThat(actual.sports?.away?.name).isEqualTo(stringReference("Away"))
            assertThat(actual.sports?.isLive).isEqualTo(model.shouldShowLiveBadge)
            assertThat(actual.sports?.homeScore).isEqualTo("1")
            assertThat(actual.sports?.awayScore).isEqualTo("2")
            assertThat(actual.sports?.period).isEqualTo(model.game.period)
            assertThat(actual.rows).hasSize(1)
            assertThat(actual.rows.single().outcomes.map { it.assetId }).containsExactly("home", "away").inOrder()
            assertThat(clicks).containsExactly(
                Triple("event-1", "market-1", "home"), Triple("event-1", "market-1", "away"),
            ).inOrder()
        } else {
            assertThat(actual.withoutActions()).isEqualTo(regular.withoutActions())
            assertThat(clicks).containsExactlyElementsIn(
                model.event.markets.flatMap { market ->
                    market.outcomes.map { Triple(model.event.id, market.id, it.assetId) }
                },
            ).inOrder()
        }
    }

    private fun provideTestModels(): List<Model> {
        val match = createPolymarketEvent(
            markets = listOf(createPolymarketMarket(outcomes = listOf(
                createPolymarketOutcome(assetId = "away", title = "Away", probability = BigDecimal("0.4")),
                createPolymarketOutcome(assetId = "home", title = "Home", probability = BigDecimal("0.6")),
            ))),
        ).copy(gameId = "11", teams = listOf(away, home))
        val game = PolymarketSportsState.Game(
            gameId = "11", score = PolymarketSportsState.Score(1, 2), period = "Q3", isLive = true, isEnded = false,
            home = PolymarketSportsState.Team(home, 1), away = PolymarketSportsState.Team(away, 0), isStale = false,
        )
        val grouped = match.copy(
            displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
            totalMarketsCount = 3,
            markets = listOf("Away", "Draw (Home vs Away)", "Home").map { label ->
                createPolymarketMarket(
                    id = label, groupItemTitle = label,
                    outcomes = listOf(
                        createPolymarketOutcome(assetId = "$label-yes", title = "Yes"),
                        createPolymarketOutcome(assetId = "$label-no", title = "No"),
                    ),
                )
            },
        )
        val fixtures = listOf(
            Fixture("complete", match, game, isMoneyline = true),
            Fixture("partial", match.copy(totalMarketsCount = 3), game),
            Fixture("unknown", match.copy(markets = createPolymarketEvent().markets), game),
            Fixture("ambiguous", match.copy(teams = listOf(away.copy(alias = "Home"), home)), game),
            Fixture("partial grouped", grouped.copy(totalMarketsCount = 4), game),
            Fixture("two grouped markets", grouped.copy(totalMarketsCount = 2, markets = grouped.markets.take(2)), game),
            Fixture("No before Yes", grouped.copy(markets = grouped.markets.map { it.copy(outcomes = it.outcomes.reversed()) }), game),
            Fixture(
                "unrelated third market",
                grouped.copy(markets = grouped.markets.map {
                    if (it.id == "Draw (Home vs Away)") it.copy(groupItemTitle = "Total goals") else it
                }),
                game,
            ),
            Fixture("missing ordering resolved by domain", match.copy(teams = listOf(away, home.copy(ordering = null))), game, isMoneyline = true),
            Fixture("different game", match.copy(gameId = "22"), game),
        )
        return fixtures.flatMap { fixture ->
            Stage.entries.flatMap { stage ->
                listOf(false, true).map { isEnabled ->
                    Model(
                        name = "${fixture.name} / $stage / D2=$isEnabled",
                        event = fixture.event.copy(isLive = stage == Stage.Live || stage == Stage.Interrupted, isEnded = stage == Stage.Ended),
                        game = fixture.game.copy(
                            isLive = stage == Stage.Live || stage == Stage.Interrupted,
                            isEnded = stage == Stage.Ended,
                            period = when (stage) {
                                Stage.Ended -> "FT"
                                Stage.Interrupted -> "INT"
                                else -> "Q3"
                            },
                        ),
                        isEnabled = isEnabled,
                        shouldShowLiveBadge = stage == Stage.Live,
                        shouldShowSports = isEnabled && fixture.isMoneyline && stage != Stage.Prematch,
                    )
                }
            }
        }
    }

    private fun PolymarketEventUM.withoutActions(): PolymarketEventUM = copy(
        onClick = noAction,
        rows = rows.map { row ->
            row.copy(outcomes = row.outcomes.map { it.copy(onClick = noAction) }.toImmutableList())
        }.toImmutableList(),
    )

    private enum class Stage { Prematch, Live, Interrupted, Ended }

    private data class Fixture(
        val name: String,
        val event: PolymarketEvent,
        val game: PolymarketSportsState.Game,
        val isMoneyline: Boolean = false,
    )

    data class Model(
        val name: String,
        val event: PolymarketEvent,
        val game: PolymarketSportsState.Game,
        val isEnabled: Boolean,
        val shouldShowLiveBadge: Boolean,
        val shouldShowSports: Boolean,
    )
}