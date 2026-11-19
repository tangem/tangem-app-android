package com.tangem.features.polymarket.impl.crypto.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketCryptoEventStateTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN window with both prices WHEN the clock moves THEN the winner is named only once it has ended`(
        model: Model,
    ) {
        // Arrange
        val event = createCryptoEvent(index = 1, priceToBeat = "100".toBigDecimal(), finalPrice = "105".toBigDecimal())
        val state = PolymarketCryptoEventState.Content(
            windows = requireNotNull(PolymarketCryptoWindows.of(event)),
            nowMillis = model.nowMillis,
            points = emptyList(),
            isPriceUnavailable = false,
            quotes = emptyMap(),
        )

        // Act
        val actual = state.phase to state.winner?.outcome?.title

        // Assert
        assertThat(actual).isEqualTo(model.phase to model.winner)
    }

    private fun provideTestModels() = listOf(
        Model(nowMillis = duringCryptoWindow(0), phase = PolymarketCryptoWindowPhase.UPCOMING, winner = null),
        Model(nowMillis = duringCryptoWindow(1), phase = PolymarketCryptoWindowPhase.LIVE, winner = null),
        Model(nowMillis = cryptoWindowStart(2), phase = PolymarketCryptoWindowPhase.ENDED, winner = "Up"),
    )

    data class Model(val nowMillis: Long, val phase: PolymarketCryptoWindowPhase, val winner: String?)
}