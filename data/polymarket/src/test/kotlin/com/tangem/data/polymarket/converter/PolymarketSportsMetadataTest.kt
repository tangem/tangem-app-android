package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.grow.datasource.polymarket.models.PolymarketEventDto
import com.tangem.grow.datasource.polymarket.models.PolymarketEventResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketEventsResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketPositionResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketPositionsResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketSearchResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketTeamDto
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Instant

internal class PolymarketSportsMetadataTest {

    private val moshi = Moshi.Builder().build()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Events {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN BFF sports metadata WHEN an event is loaded THEN the domain retains it`(model: EventModel) {
            // Arrange
            val json = eventJson(model.sports.json + model.sports.teamsJson)
            val dto = when (model.endpoint) {
                EventEndpoint.FEED -> moshi.adapter(PolymarketEventsResponse::class.java)
                    .fromJson("""{"events":[$json],"cursor":null,"hasNext":false}""")!!.events.single()
                EventEndpoint.DETAILS -> moshi.adapter(PolymarketEventResponse::class.java)
                    .fromJson("""{"event":$json}""")!!.event
                EventEndpoint.SEARCH -> moshi.adapter(PolymarketSearchResponse::class.java)
                    .fromJson("""{"events":[$json],"page":1,"total":1,"hasNext":false}""")!!.events.single()
            }

            // Act
            val actual = PolymarketEventConverter.convert(dto)

            // Assert
            assertThat(actual.isLive).isEqualTo(model.sports.isLive)
            assertThat(actual.gameId).isEqualTo(model.sports.gameId)
            assertThat(actual.score).isEqualTo(model.sports.score)
            assertThat(actual.period).isEqualTo(model.sports.period)
            assertThat(actual.isEnded).isEqualTo(model.sports.isEnded)
            assertThat(actual.dataFetchedAt).isEqualTo(model.sports.dataFetchedAt)
            assertThat(actual.teams).isEqualTo(model.sports.teams)
        }

        private fun provideTestModels(): List<EventModel> = EventEndpoint.entries.flatMap { endpoint ->
            sportsCases().map { EventModel(endpoint, it) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Positions {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN BFF sports metadata WHEN a position is loaded THEN the domain retains it`(model: PositionModel) {
            // Arrange
            val json = positionJson(model.sports.json)
            val dto = when (model.endpoint) {
                PositionEndpoint.LIST -> moshi.adapter(PolymarketPositionsResponse::class.java)
                    .fromJson("""{"positions":[$json],"hasNext":false}""")!!.positions.single()
                PositionEndpoint.SINGLE -> moshi.adapter(PolymarketPositionResponse::class.java)
                    .fromJson("""{"position":$json}""")!!.position!!
            }

            // Act
            val actual = PolymarketOpenPositionConverter.convert(dto)

            // Assert
            assertThat(actual.isLive).isEqualTo(model.sports.isLive)
            assertThat(actual.gameId).isEqualTo(model.sports.gameId)
            assertThat(actual.score).isEqualTo(model.sports.score)
            assertThat(actual.period).isEqualTo(model.sports.period)
            assertThat(actual.isEnded).isEqualTo(model.sports.isEnded)
            assertThat(actual.dataFetchedAt).isEqualTo(model.sports.dataFetchedAt)
        }

        @Test
        fun `GIVEN no holding WHEN a single position is loaded THEN the position stays absent`() {
            // Arrange
            val json = """{"position":null}"""

            // Act
            val actual = moshi.adapter(PolymarketPositionResponse::class.java).fromJson(json)

            // Assert
            assertThat(actual!!.position).isNull()
        }

        private fun provideTestModels(): List<PositionModel> = PositionEndpoint.entries.flatMap { endpoint ->
            sportsCases().map { PositionModel(endpoint, it) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RequiredFields {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an absent required field WHEN decoded THEN the contract violation is reported`(model: RequiredModel) {
            // Act
            val error = runCatching { moshi.adapter(model.type).fromJson(model.json) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(JsonDataException::class.java)
            assertThat(error).hasMessageThat().contains(model.field)
        }

        private fun provideTestModels(): List<RequiredModel> = listOf(
            RequiredModel(PolymarketEventDto::class.java, eventJson(), "live"),
            RequiredModel(PolymarketEventDto::class.java, eventJson(""", "live":null"""), "live"),
            RequiredModel(PolymarketTeamDto::class.java, "{}", "name"),
            RequiredModel(PolymarketTeamDto::class.java, """{"name":null}""", "name"),
        )
    }

    private fun sportsCases(): List<SportsCase> = listOf(
        SportsCase(
            json = """
                , "live":true, "gameId":"090108924", "score":"2-6, 5-3", "period":"S2", "ended":false,
                "dataFetchedAt":"2026-10-02T12:00:00.123Z"
            """.trimIndent(),
            teamsJson = """
                , "teams":[
                  {"name":"Away player","abbreviation":"AWY","alias":"A. Player",
                   "logo":"https://example.com/away.png","ordering":"away"},
                  {"name":"Home player","abbreviation":null,"alias":null,"logo":null,"ordering":"home"},
                  {"name":"Third participant"}
                ]
            """.trimIndent(),
            isLive = true,
            gameId = "090108924",
            score = "2-6, 5-3",
            period = "S2",
            isEnded = false,
            dataFetchedAt = Instant.parse("2026-10-02T12:00:00.123Z"),
            teams = listOf(
                PolymarketTeam("Away player", "AWY", "A. Player", "https://example.com/away.png", "away"),
                PolymarketTeam("Home player", null, null, null, "home"),
                PolymarketTeam("Third participant", null, null, null, null),
            ),
        ),
        SportsCase(
            json = """
                , "live":false, "gameId":"90108924", "score":"2-1", "period":"FT", "ended":true,
                "dataFetchedAt":"2026-10-02T12:00:00Z"
            """.trimIndent(),
            teamsJson = """, "teams":[]""",
            gameId = "90108924",
            score = "2-1",
            period = "FT",
            isEnded = true,
            dataFetchedAt = Instant.parse("2026-10-02T12:00:00Z"),
            teams = emptyList(),
        ),
        SportsCase(json = """, "live":false"""),
        SportsCase(
            json = """
                , "live":false, "gameId":null, "score":null, "period":null, "ended":null,
                "dataFetchedAt":null
            """.trimIndent(),
            teamsJson = """, "teams":null""",
        ),
    )

    private fun eventJson(metadata: String = ""): String = """
        {
          "eventId":"event-1", "slug":"event", "title":"Event", "description":"Description",
          "polymarketRulesUrl":"https://polymarket.com/rules", "status":"active",
          "totalMarketsCount":0, "negRisk":false, "displayMode":"plain_markets", "markets":[]
          $metadata
        }
    """.trimIndent()

    private fun positionJson(metadata: String = ""): String = """
        {
          "conditionId":"condition-1", "assetId":"asset-1", "title":"Position", "slug":"position",
          "outcome":"Yes", "outcomeIndex":0, "size":"10", "avgPrice":"0.3", "curPrice":"0.4",
          "initialValue":"3", "currentValue":"4", "cashPnl":"1", "percentPnl":"33.33",
          "realizedPnl":"0", "totalBought":"10", "redeemable":false, "negRisk":false,
          "acceptingOrders":true, "resolutionStatus":"UNRESOLVED"
          $metadata
        }
    """.trimIndent()

    internal enum class EventEndpoint { FEED, DETAILS, SEARCH }
    internal enum class PositionEndpoint { LIST, SINGLE }

    internal data class EventModel(val endpoint: EventEndpoint, val sports: SportsCase)
    internal data class PositionModel(val endpoint: PositionEndpoint, val sports: SportsCase)
    internal data class RequiredModel(val type: Class<*>, val json: String, val field: String)

    internal data class SportsCase(
        val json: String,
        val teamsJson: String = "",
        val isLive: Boolean = false,
        val gameId: String? = null,
        val score: String? = null,
        val period: String? = null,
        val isEnded: Boolean? = null,
        val dataFetchedAt: Instant? = null,
        val teams: List<PolymarketTeam>? = null,
    )
}