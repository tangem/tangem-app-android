package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketLiveScoreConverterTest {

    private val received = Instant.parse("2026-10-19T12:00:00Z")
    private val source = Instant.parse("2026-10-19T11:59:00Z")

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN optional source timestamps WHEN converting THEN source and reception times stay separate`(model: Model) {
        // Arrange
        val input = PolymarketLiveScoreConverter.Input(
            SportsResultDto(1, "2-1", "Q3", true, false, "Alpha", "Beta", model.value), "01", received,
        )

        // Act
        val actual = PolymarketLiveScoreConverter.convert(input)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketLiveScore("01", "2-1", "Q3", true, false, "Alpha", "Beta", model.expected, received),
        )
    }

    private fun provideTestModels() = listOf(
        Model(JsonPrimitive(source.toString()), source),
        Model(JsonPrimitive(source.epochSecond), source),
        Model(JsonPrimitive(source.toEpochMilli().toString()), source),
        Model(JsonPrimitive("2026-10-19T14:59:00+03:00"), source),
        Model(null, received), Model(JsonNull, received), Model(JsonObject(emptyMap()), received),
        Model(JsonPrimitive("invalid"), received), Model(JsonPrimitive(-1), received),
        Model(JsonPrimitive("9999999999999999999999999999"), received),
    )

    data class Model(val value: JsonElement?, val expected: Instant)
}