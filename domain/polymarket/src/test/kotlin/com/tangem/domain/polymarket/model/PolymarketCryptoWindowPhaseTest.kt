package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase.ENDED
import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase.LIVE
import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase.UPCOMING
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketCryptoWindowPhaseTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN moment of the clock WHEN phase is read THEN the window is live from its start up to its end`(
        model: Model,
    ) {
        // Arrange
        val window = PolymarketCryptoWindow(
            start = Instant.ofEpochMilli(START_MILLIS),
            end = Instant.ofEpochMilli(END_MILLIS),
            priceFeed = null,
            priceToBeat = null,
            finalPrice = null,
            nextWindows = emptyList(),
            liveEventId = null,
        )

        // Act
        val actual = window.phaseAt(model.nowMillis)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        Model(nowMillis = 0, expected = UPCOMING),
        Model(nowMillis = START_MILLIS - 1, expected = UPCOMING),
        Model(nowMillis = START_MILLIS, expected = LIVE),
        Model(nowMillis = END_MILLIS - 1, expected = LIVE),
        Model(nowMillis = END_MILLIS, expected = ENDED),
        Model(nowMillis = END_MILLIS + 1, expected = ENDED),
    )

    data class Model(val nowMillis: Long, val expected: PolymarketCryptoWindowPhase)

    private companion object {
        const val START_MILLIS = 1_000L
        const val END_MILLIS = 301_000L
    }
}