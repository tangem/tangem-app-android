package com.tangem.features.foryou.impl.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.model.ForYouPeriod
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class ForYouUtilsTest {

    @Nested
    inner class ForYouGroupKey {

        @Test
        fun `GIVEN standard currency with raw id WHEN forYouGroupKey THEN returns rawCurrencyId value`() {
            // Arrange
            val id: CryptoCurrency.ID = mockk {
                every { rawCurrencyId } returns CryptoCurrency.RawID("bitcoin")
                every { value } returns "coin-id-value"
            }
            val currency: CryptoCurrency = mockk { every { this@mockk.id } returns id }
            val status = createStatus(currency)

            // Act
            val result = status.forYouGroupKey()

            // Assert
            assertThat(result).isEqualTo("bitcoin")
        }

        @Test
        fun `GIVEN custom token with no raw id WHEN forYouGroupKey THEN falls back to currency id value`() {
            // Arrange
            val id: CryptoCurrency.ID = mockk {
                every { rawCurrencyId } returns null
                every { value } returns "custom-currency-id"
            }
            val currency: CryptoCurrency = mockk { every { this@mockk.id } returns id }
            val status = createStatus(currency)

            // Act
            val result = status.forYouGroupKey()

            // Assert
            assertThat(result).isEqualTo("custom-currency-id")
        }

        private fun createStatus(currency: CryptoCurrency): CryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loading,
        )
    }

    @Nested
    inner class ForYouEarnAssetKey {

        @Test
        fun `GIVEN currency with raw id WHEN forYouEarnAssetKey THEN key is raw id to network raw id`() {
            // Arrange
            val currency = createCurrency(rawCurrencyId = "usd-coin", currencyId = "token-usdc", networkRawId = "ETH")

            // Act
            val result = currency.forYouEarnAssetKey()

            // Assert
            assertThat(result).isEqualTo("usd-coin" to "ETH")
        }

        @Test
        fun `GIVEN custom token with no raw id WHEN forYouEarnAssetKey THEN falls back to currency id value`() {
            // Arrange
            val currency = createCurrency(rawCurrencyId = null, currencyId = "custom-token-id", networkRawId = "ETH")

            // Act
            val result = currency.forYouEarnAssetKey()

            // Assert
            assertThat(result).isEqualTo("custom-token-id" to "ETH")
        }

        @Test
        fun `GIVEN same asset on different networks WHEN forYouEarnAssetKey THEN keys differ`() {
            // Arrange
            val onEthereum = createCurrency(rawCurrencyId = "usd-coin", currencyId = "usdc-eth", networkRawId = "ETH")
            val onSolana = createCurrency(rawCurrencyId = "usd-coin", currencyId = "usdc-sol", networkRawId = "SOL")

            // Act & Assert
            assertThat(onEthereum.forYouEarnAssetKey()).isNotEqualTo(onSolana.forYouEarnAssetKey())
        }

        private fun createCurrency(
            rawCurrencyId: String?,
            currencyId: String,
            networkRawId: String,
        ): CryptoCurrency {
            val id: CryptoCurrency.ID = mockk {
                every { this@mockk.rawCurrencyId } returns rawCurrencyId?.let { CryptoCurrency.RawID(it) }
                every { value } returns currencyId
            }
            val network: Network = mockk {
                every { rawId } returns networkRawId
            }
            return mockk {
                every { this@mockk.id } returns id
                every { this@mockk.network } returns network
            }
        }
    }

    @Nested
    inner class ToForYouPercent {

        @Test
        fun `GIVEN null amount WHEN toForYouPercent THEN returns null`() {
            // Arrange
            val amount: BigDecimal? = null

            // Act
            val result = amount.toForYouPercent(BigDecimal("100"))

            // Assert
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN zero total WHEN toForYouPercent THEN returns null`() {
            // Arrange
            val amount = BigDecimal("10")

            // Act
            val result = amount.toForYouPercent(BigDecimal.ZERO)

            // Assert
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN zero amount WHEN toForYouPercent THEN returns null`() {
            // Arrange
            val amount = BigDecimal.ZERO

            // Act
            val result = amount.toForYouPercent(BigDecimal("100"))

            // Assert
            assertThat(result).isNull()
        }

        @Test
        fun `GIVEN non-zero amount and total WHEN toForYouPercent THEN returns the share as a ratio`() {
            // Arrange — 50.00 / 200 = 0.25 (ratio, scaled to the amount's scale)
            val amount = BigDecimal("50.00")

            // Act
            val result = amount.toForYouPercent(BigDecimal("200"))

            // Assert
            assertThat(result).isEqualTo(BigDecimal("0.25"))
        }

        @Test
        fun `GIVEN a share requiring rounding WHEN toForYouPercent THEN applies HALF_UP rounding`() {
            // Arrange — 1.0000 / 3 = 0.3333... rounds HALF_UP to the amount's scale (4)
            val amount = BigDecimal("1.0000")

            // Act
            val result = amount.toForYouPercent(BigDecimal("3"))

            // Assert
            assertThat(result).isEqualTo(BigDecimal("0.3333"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ForYouSentimentBadge {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN readings WHEN forYouSentimentBadge THEN badge matches the net signal score`(model: BadgeModel) {
            // Act
            val result = forYouSentimentBadge(coinIndicators = model.coinIndicators, timeframe = model.timeframe)

            // Assert
            if (model.expected == null) {
                assertThat(result).isNull()
            } else {
                assertThat(result).isNotNull()
                assertThat(result!!.text).isEqualTo(model.expected.first)
                assertThat(result.color).isEqualTo(model.expected.second)
            }
        }

        private fun provideTestModels() = listOf(
            // No entry for the symbol at all → no badge
            BadgeModel(coinIndicators = null, expected = null),
            // Entry present but without readings → score 0 → Neutral (summary shows "Neutral outlook" too)
            BadgeModel(coinIndicators = createIndicators(), expected = resourceReference(R.string.common_neutral) to TangemBadgeColor.Blue),
            // Only non-actionable signals → score 0 → Neutral, matching the summary's "Neutral outlook"
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.RSI, Signal.INSUFFICIENT_DATA, Timeframe.DAY),
                    createReading(CoinIndicators.Reading.Type.SENTIMENT, Signal.NOT_AVAILABLE),
                    createReading(CoinIndicators.Reading.Type.MA_CROSS, Signal.NOT_AVAILABLE),
                ),
                expected = resourceReference(R.string.common_neutral) to TangemBadgeColor.Blue,
            ),
            // Net-positive score → Positive
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.RSI, Signal.POSITIVE, Timeframe.DAY),
                    createReading(CoinIndicators.Reading.Type.MACD, Signal.POSITIVE, Timeframe.DAY),
                    createReading(CoinIndicators.Reading.Type.SENTIMENT, Signal.NEGATIVE),
                ),
                expected = resourceReference(R.string.common_positive) to TangemBadgeColor.Green,
            ),
            // Net-negative score → Negative
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.RSI, Signal.POSITIVE, Timeframe.DAY),
                    createReading(CoinIndicators.Reading.Type.MACD, Signal.NEGATIVE, Timeframe.DAY),
                    createReading(CoinIndicators.Reading.Type.MA_CROSS, Signal.NEGATIVE),
                ),
                expected = resourceReference(R.string.common_negative) to TangemBadgeColor.Red,
            ),
            // Balanced score → Neutral
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.RSI, Signal.POSITIVE, Timeframe.DAY),
                    createReading(CoinIndicators.Reading.Type.MACD, Signal.NEGATIVE, Timeframe.DAY),
                ),
                expected = resourceReference(R.string.common_neutral) to TangemBadgeColor.Blue,
            ),
            // All-neutral actionable signals → Neutral (unlike non-actionable, they do produce a badge)
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.GALAXY_SCORE, Signal.NEUTRAL),
                ),
                expected = resourceReference(R.string.common_neutral) to TangemBadgeColor.Blue,
            ),
            // The only reading belongs to another timeframe → nothing scores for DAY → Neutral
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.RSI, Signal.POSITIVE, Timeframe.WEEK),
                ),
                expected = resourceReference(R.string.common_neutral) to TangemBadgeColor.Blue,
            ),
            // WEEK selection picks the WEEK reading of RSI, not the DAY one
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.RSI, Signal.POSITIVE, Timeframe.DAY),
                    createReading(CoinIndicators.Reading.Type.RSI, Signal.NEGATIVE, Timeframe.WEEK),
                ),
                timeframe = Timeframe.WEEK,
                expected = resourceReference(R.string.common_negative) to TangemBadgeColor.Red,
            ),
            // Social indicators are keyed by timeframe too: the MONTH reading scores for a MONTH selection
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.SENTIMENT, Signal.POSITIVE, Timeframe.MONTH),
                ),
                timeframe = Timeframe.MONTH,
                expected = resourceReference(R.string.common_positive) to TangemBadgeColor.Green,
            ),
            // …and no longer count outside it: a DAY-only SENTIMENT reading scores nothing for MONTH
            BadgeModel(
                coinIndicators = createIndicators(
                    createReading(CoinIndicators.Reading.Type.SENTIMENT, Signal.POSITIVE, Timeframe.DAY),
                ),
                timeframe = Timeframe.MONTH,
                expected = resourceReference(R.string.common_neutral) to TangemBadgeColor.Blue,
            ),
        )

        @Test
        fun `GIVEN actionable readings WHEN forYouSentimentBadge THEN badge keeps the row title style`() {
            // Arrange
            val indicators = createIndicators(createReading(CoinIndicators.Reading.Type.SENTIMENT, Signal.POSITIVE))

            // Act
            val result = forYouSentimentBadge(coinIndicators = indicators, timeframe = Timeframe.DAY)

            // Assert
            assertThat(result).isNotNull()
            assertThat(result!!.size).isEqualTo(TangemBadgeSize.X4)
            assertThat(result.type).isEqualTo(TangemBadgeType.Tinted)
        }
    }

    @Nested
    inner class ForYouPeriodFromId {

        @Test
        fun `GIVEN known segment id WHEN fromId THEN returns the matching period`() {
            // Act & Assert
            assertThat(ForYouPeriod.fromId("1")).isEqualTo(ForYouPeriod.Week)
            assertThat(ForYouPeriod.fromId("2")).isEqualTo(ForYouPeriod.Month)
        }

        @Test
        fun `GIVEN unknown or null id WHEN fromId THEN falls back to Day`() {
            // Act & Assert
            assertThat(ForYouPeriod.fromId("42")).isEqualTo(ForYouPeriod.Day)
            assertThat(ForYouPeriod.fromId(null)).isEqualTo(ForYouPeriod.Day)
        }
    }

    @Nested
    inner class ForYouPeriodEntries {

        @Test
        fun `GIVEN ForYouPeriod entries THEN each maps to its segment id title and timeframe`() {
            // Pins the enum's segment ids (portable to TokenSummaryModel), titles and timeframes.
            // Act
            val mapping = ForYouPeriod.entries.map { Triple(it.id, it.title, it.timeframe) }

            // Assert
            assertThat(mapping).containsExactly(
                Triple("0", resourceReference(R.string.common_day), CoinIndicators.Reading.Timeframe.DAY),
                Triple("1", resourceReference(R.string.common_week), CoinIndicators.Reading.Timeframe.WEEK),
                Triple("2", resourceReference(R.string.common_month), CoinIndicators.Reading.Timeframe.MONTH),
            ).inOrder()
        }
    }

    internal data class BadgeModel(
        val coinIndicators: CoinIndicators?,
        val timeframe: CoinIndicators.Reading.Timeframe = CoinIndicators.Reading.Timeframe.DAY,
        val expected: Pair<TextReference, TangemBadgeColor>?,
    )

    private fun createIndicators(vararg readings: CoinIndicators.Reading): CoinIndicators = CoinIndicators(
        symbol = "BTC",
        readings = readings.toList(),
    )

    private fun createReading(
        type: CoinIndicators.Reading.Type,
        signal: CoinIndicators.Reading.Signal,
        timeframe: CoinIndicators.Reading.Timeframe = CoinIndicators.Reading.Timeframe.DAY,
    ): CoinIndicators.Reading = CoinIndicators.Reading(
        type = type,
        name = type.name,
        timeframe = timeframe,
        value = null,
        signal = signal,
        updatedAt = null,
    )
}

private typealias Signal = CoinIndicators.Reading.Signal
private typealias Timeframe = CoinIndicators.Reading.Timeframe