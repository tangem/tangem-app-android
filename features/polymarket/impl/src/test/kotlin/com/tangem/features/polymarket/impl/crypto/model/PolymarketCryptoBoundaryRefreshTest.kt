package com.tangem.features.polymarket.impl.crypto.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketCryptoBoundaryRefreshTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN window and its last read WHEN claimed THEN it is read after a bound and while its number is missing`(
        model: Model,
    ) {
        // Arrange
        val event = createCryptoEvent(
            index = 1,
            priceToBeat = PRICE.takeIf { model.hasPriceToBeat },
            finalPrice = PRICE.takeIf { model.hasFinalPrice },
            outcomes = listOf(
                createPolymarketOutcome(assetId = "up", title = "Up", probability = model.upProbability),
                createPolymarketOutcome(assetId = "down", title = "Down"),
            ),
        )

        val expected = PolymarketCryptoBoundaryRefresh(lastReadMillis = model.nowMillis).takeIf { model.isDue }

        // Act
        val actual = PolymarketCryptoBoundaryRefresh(model.lastReadMillis).claim(event, model.nowMillis)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        Model(case = "upcoming", lastReadMillis = 0, nowMillis = START - 1, isDue = false),
        Model(case = "just started", lastReadMillis = 0, nowMillis = START + 1_999, isDue = false),
        Model(case = "first read after the start", lastReadMillis = START - 1, nowMillis = START + 2_000),
        Model(
            case = "read after the start, price to beat known",
            hasPriceToBeat = true,
            lastReadMillis = START + 2_000,
            nowMillis = START + 60_000,
            isDue = false,
        ),
        Model(
            case = "price to beat missing, retry not yet due",
            lastReadMillis = START + 2_000,
            nowMillis = START + 6_999,
            isDue = false,
        ),
        Model(case = "price to beat missing, retry", lastReadMillis = START + 2_000, nowMillis = START + 7_000),
        Model(
            case = "opened during the window without the price to beat",
            lastReadMillis = START + 30_000,
            nowMillis = START + 35_000,
        ),
        Model(
            case = "price to beat missing after two minutes, slow retry not yet due",
            lastReadMillis = START + 120_000,
            nowMillis = START + 139_999,
            isDue = false,
        ),
        Model(
            case = "price to beat missing after two minutes, slow retry",
            lastReadMillis = START + 120_000,
            nowMillis = START + 140_000,
        ),
        Model(
            case = "just ended, the start still rules",
            hasPriceToBeat = true,
            lastReadMillis = START + 2_000,
            nowMillis = END + 1_999,
            isDue = false,
        ),
        Model(
            case = "first read after the end",
            hasPriceToBeat = true,
            lastReadMillis = END - 1,
            nowMillis = END + 2_000,
        ),
        Model(
            case = "ended and settled",
            hasPriceToBeat = true,
            hasFinalPrice = true,
            upProbability = BigDecimal.ONE,
            lastReadMillis = END + 2_000,
            nowMillis = END + 60_000,
            isDue = false,
        ),
        Model(
            case = "ended and settled, the start no longer counts",
            hasFinalPrice = true,
            upProbability = BigDecimal.ONE,
            lastReadMillis = END + 2_000,
            nowMillis = END + 60_000,
            isDue = false,
        ),
        Model(
            case = "read at the very end is not a read before it",
            hasPriceToBeat = true,
            lastReadMillis = END,
            nowMillis = END + 2_000,
            isDue = false,
        ),
        Model(
            case = "final price known, not settled, retry",
            hasPriceToBeat = true,
            hasFinalPrice = true,
            lastReadMillis = END + 2_000,
            nowMillis = END + 7_000,
        ),
        Model(
            case = "settled without the final price, retry",
            hasPriceToBeat = true,
            upProbability = BigDecimal.ONE,
            lastReadMillis = END + 2_000,
            nowMillis = END + 7_000,
        ),
        Model(
            case = "nothing after the end, retry not yet due",
            lastReadMillis = END + 2_000,
            nowMillis = END + 6_999,
            isDue = false,
        ),
        Model(
            case = "opened after the end without the result",
            lastReadMillis = END + 30_000,
            nowMillis = END + 35_000,
        ),
        Model(
            case = "two minutes after the end, slow retry not yet due",
            lastReadMillis = END + 120_000,
            nowMillis = END + 139_999,
            isDue = false,
        ),
        Model(
            case = "two minutes after the end, slow retry",
            lastReadMillis = END + 120_000,
            nowMillis = END + 140_000,
        ),
        Model(
            case = "last retry before ten minutes",
            lastReadMillis = END + 579_999,
            nowMillis = END + 599_999,
        ),
        Model(
            case = "ten minutes after the end, given up",
            lastReadMillis = END + 580_000,
            nowMillis = END + 600_000,
            isDue = false,
        ),
        Model(
            case = "long after the end, given up",
            lastReadMillis = END + 3_600_000,
            nowMillis = END + 7_200_000,
            isDue = false,
        ),
    )

    @Test
    fun `GIVEN window an hour long WHEN the price to beat is missing ten minutes in THEN retries are given up`() {
        // Arrange
        val event = createCryptoEvent(index = 0).let { event ->
            val window = requireNotNull(event.cryptoWindow)
            event.copy(cryptoWindow = window.copy(end = window.start.plusSeconds(HOUR_SECONDS)))
        }

        // Act
        val beforeLimit = PolymarketCryptoBoundaryRefresh(lastReadMillis = 579_999).claim(event, nowMillis = 599_999)
        val atLimit = PolymarketCryptoBoundaryRefresh(lastReadMillis = 580_000).claim(event, nowMillis = 600_000)

        // Assert
        assertThat(beforeLimit).isEqualTo(PolymarketCryptoBoundaryRefresh(lastReadMillis = 599_999))
        assertThat(atLimit).isNull()
    }

    @Test
    fun `GIVEN read claimed after the end WHEN its answer is slow THEN the next is due only with the retry`() {
        // Arrange
        val event = createCryptoEvent(index = 1)
        val claimed = requireNotNull(PolymarketCryptoBoundaryRefresh(lastReadMillis = START).claim(event, END + 2_000))

        // Act
        val onNextTick = claimed.claim(event, nowMillis = END + 3_000)
        val onRetry = claimed.claim(event, nowMillis = END + 7_000)

        // Assert
        assertThat(onNextTick).isNull()
        assertThat(onRetry).isEqualTo(PolymarketCryptoBoundaryRefresh(lastReadMillis = END + 7_000))
    }

    @Test
    fun `GIVEN event without a window WHEN claimed THEN it is never read for a bound`() {
        // Act
        val actual = PolymarketCryptoBoundaryRefresh(lastReadMillis = 0).claim(createPolymarketEvent(), END + 2_000)

        // Assert
        assertThat(actual).isNull()
    }

    data class Model(
        val case: String,
        val hasPriceToBeat: Boolean = false,
        val hasFinalPrice: Boolean = false,
        val upProbability: BigDecimal? = null,
        val lastReadMillis: Long,
        val nowMillis: Long,
        val isDue: Boolean = true,
    )

    private companion object {
        val START = cryptoWindowStart(index = 1)
        val END = cryptoWindowStart(index = 2)
        val PRICE: BigDecimal = BigDecimal("64504.94")
        const val HOUR_SECONDS = 3_600L
    }
}