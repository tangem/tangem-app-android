package com.tangem.domain.polymarket.sports

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSportsState.Score
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketScoreParserTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN provider score WHEN parsed THEN home away numbers follow its format`(model: Model) {
        // Arrange
        val input = model.raw

        // Act
        val actual = PolymarketScoreParser.parse(input, model.period)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        Model("2-1", Score(2, 1)), Model("14-7", Score(14, 7)), Model(" 002 - 01 ", Score(2, 1)),
        Model("000-000|1-1|Bo3", Score(1, 1)), Model("000-000|2-0|Bo3", Score(2, 0)),
        Model("6-7(4-7), 6-2, 0-2", Score(1, 1)), Model("7-6(4), 6-5", Score(1, 0)),
        Model("6-4, 4-6, 8-6", Score(2, 1)), Model("6-6, 5-4", Score(0, 0)),
        Model("6-7(4-7)", Score(0, 1)), Model("6-2", Score(6, 2)),
        Model("3-2", Score(0, 0), period = "S1"), Model("6-4", Score(1, 0), period = "S1"),
        Model("6-4, 2-3", Score(1, 0), period = "S2"), Model("7-6(4)", Score(1, 0), period = "S1"),
        Model("6-6", Score(0, 0), period = "S1"), Model("000-000|1-1|Bo3", Score(1, 1), period = "S1"),
        Model("3-2", Score(3, 2), period = "Q1"), Model("3-2", Score(3, 2), period = ""),
        Model("3-2", Score(3, 2), period = "S"), Model("3-2", Score(3, 2), period = "S0"),
        Model("3-2", Score(3, 2), period = "S-1"), Model("3-2", Score(3, 2), period = "Suspended"),
        Model(null, null, period = "S1"), Model("unknown", null, period = "S1"),
        Model(null, null), Model("", null), Model("2:1", null), Model("-1-0", null),
        Model("000-000||Bo3", null), Model("6-4, garbage", null), Model("6-7(foo)", null),
        Model("999999999999999999999-0", null), Model("1-0 trailing", null),
    )

    data class Model(val raw: String?, val expected: Score?, val period: String? = null)
}