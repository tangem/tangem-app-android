package com.tangem.features.feed.crypto.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.MarketIndicators
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.ui.state.MarketIndexStatus
import com.tangem.features.feed.crypto.ui.state.MarketIndexUM
import com.tangem.features.feed.crypto.ui.state.TotalMarketCapUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.DASH_SIGN
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

/**
 * The capitalisation goes through `compact()`, which delegates to `android.icu.text.CompactDecimalFormat`
 * — a framework class the JVM test runtime does not provide. `compact()` short-circuits to the JDK
 * formatter for amounts below `1`, so the fixtures use a token amount: these tests cover the mapping,
 * and the compact rendering of a realistic capitalisation is checked on device.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TotalMarketCapConverterTest {

    private val converter =
        TotalMarketCapConverter(appCurrency = AppCurrency.Default, onIndexClick = {}, onInfoClick = {})

    private lateinit var defaultLocale: Locale

    @BeforeAll
    fun pinLocale() {
        // both formatters render through `Locale.getDefault()`, so the expected strings only hold once
        // it is pinned — on a de/fr/ru host they come back as `0,50 $` and `0,93 %`
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterAll
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN indicators WHEN convert THEN the headline carries the amount in the selected currency`() {
        // Act
        val actual = converter.convert(indicatorsOf()) as TotalMarketCapUM.Content

        // Assert
        assertThat(actual.title).isEqualTo(TextReference.Res(R.string.market_crypto_total_market_cap_title))
        assertThat(actual.value).isEqualTo(TextReference.Str("\$0.50"))
    }

    @Test
    fun `GIVEN both indices WHEN convert THEN each card carries its titles, label and scale position`() {
        // Arrange
        val indicators = indicatorsOf(
            fearGreed = MarketIndicators.Index(value = 73, classification = "Greed"),
            altcoinSeason = MarketIndicators.Index(value = 45, classification = "Balanced Market"),
        )

        // Act
        val actual = converter.convert(indicators) as TotalMarketCapUM.Content

        // Assert — the card gets the short title, the sheet the full one
        assertThat(actual.indexes.first()).isEqualTo(
            MarketIndexUM(
                id = "fear_greed",
                title = TextReference.Res(R.string.market_crypto_total_cap_fear_greed_title_short),
                fullTitle = TextReference.Res(R.string.market_crypto_total_cap_fear_greed_title_full),
                description = TextReference.Res(R.string.market_crypto_total_cap_fear_greed_info),
                statusLabel = TextReference.Str("Greed"),
                status = MarketIndexStatus.Info,
                value = TextReference.Str("73"),
                progress = 0.73f,
            ),
        )
        // the altcoin index publishes a single title key, used for both the card and the sheet
        assertThat(actual.indexes.last()).isEqualTo(
            MarketIndexUM(
                id = "altcoin_index",
                title = TextReference.Res(R.string.market_crypto_total_cap_altcoin_index_title),
                fullTitle = TextReference.Res(R.string.market_crypto_total_cap_altcoin_index_title),
                description = TextReference.Res(R.string.market_crypto_total_cap_altcoin_index_info),
                statusLabel = TextReference.Str("Balanced Market"),
                status = MarketIndexStatus.Warning,
                value = TextReference.Str("45"),
                progress = 0.45f,
            ),
        )
    }

    @Test
    fun `GIVEN a card WHEN its click is invoked THEN the converter's callback receives that card`() {
        // Arrange
        var clicked: MarketIndexUM? = null
        val converter = TotalMarketCapConverter(
            appCurrency = AppCurrency.Default,
            onIndexClick = { index -> clicked = index },
            onInfoClick = {},
        )
        val indicators = indicatorsOf(fearGreed = MarketIndicators.Index(value = 73, classification = "Greed"))
        val content = converter.convert(indicators) as TotalMarketCapUM.Content

        // Act
        content.onIndexClick(content.indexes.single())

        // Assert
        assertThat(clicked).isEqualTo(content.indexes.single())
    }

    @Test
    fun `GIVEN only one index WHEN convert THEN only its card is built`() {
        // Arrange
        val indicators = indicatorsOf(fearGreed = MarketIndicators.Index(value = 13, classification = "Extreme Fear"))

        // Act
        val actual = converter.convert(indicators) as TotalMarketCapUM.Content

        // Assert
        assertThat(actual.indexes.map { it.id }).containsExactly("fear_greed")
    }

    @Test
    fun `GIVEN no indices WHEN convert THEN the headline stands alone`() {
        // Act
        val actual = converter.convert(indicatorsOf()) as TotalMarketCapUM.Content

        // Assert
        assertThat(actual.indexes).isEmpty()
    }

    @Test
    fun `GIVEN no change WHEN convert THEN the badge is neutral and dashed`() {
        // Act
        val actual = converter.convert(indicatorsOf(marketCapChange24h = null)) as TotalMarketCapUM.Content

        // Assert
        assertThat(actual.change).isEqualTo(
            TangemPriceChange.State(
                value = TextReference.Str(DASH_SIGN),
                direction = TangemPriceChange.Direction.Neutral,
            ),
        )
    }

    @Test
    fun `GIVEN a change fraction WHEN convert THEN it is rendered as an unsigned percentage`() {
        // Arrange — the wire percentage -0.93 reaches the converter as the fraction -0.0093
        val indicators = indicatorsOf(marketCapChange24h = BigDecimal("-0.0093"))

        // Act
        val actual = converter.convert(indicators) as TotalMarketCapUM.Content

        // Assert — the arrow carries the sign, so the text does not
        assertThat(actual.change.value).isEqualTo(TextReference.Str("0.93%"))
        assertThat(actual.change.direction).isEqualTo(TangemPriceChange.Direction.Down)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoCapitalisation {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: MissingCapitalisationModel) {
            // Act
            val actual = converter.convert(indicatorsOf(totalMarketCap = model.totalMarketCap))

            // Assert — the headline is the block's identity, so there is nothing to show without it
            assertThat(actual).isEqualTo(TotalMarketCapUM.Empty)
        }

        private fun provideTestModels() = listOf(
            MissingCapitalisationModel(totalMarketCap = null),
            MissingCapitalisationModel(totalMarketCap = BigDecimal.ZERO),
            MissingCapitalisationModel(totalMarketCap = BigDecimal("0.00")),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ScaleBands {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: BandModel) {
            // Arrange
            val indicators = indicatorsOf(
                fearGreed = MarketIndicators.Index(value = model.value, classification = "Fear"),
            )

            // Act
            val card = (converter.convert(indicators) as TotalMarketCapUM.Content).indexes.single()

            // Assert
            assertThat(card.value).isEqualTo(TextReference.Str(model.value.toString()))
            assertThat(card.status).isEqualTo(model.expectedStatus)
            assertThat(card.progress).isEqualTo(model.expectedProgress)
        }

        private fun provideTestModels() = listOf(
            BandModel(value = 0, expectedStatus = MarketIndexStatus.Error, expectedProgress = 0f),
            BandModel(value = 33, expectedStatus = MarketIndexStatus.Error, expectedProgress = 0.33f),
            BandModel(value = 34, expectedStatus = MarketIndexStatus.Warning, expectedProgress = 0.34f),
            BandModel(value = 49, expectedStatus = MarketIndexStatus.Warning, expectedProgress = 0.49f),
            BandModel(value = 50, expectedStatus = MarketIndexStatus.Success, expectedProgress = 0.5f),
            BandModel(value = 66, expectedStatus = MarketIndexStatus.Success, expectedProgress = 0.66f),
            BandModel(value = 67, expectedStatus = MarketIndexStatus.Info, expectedProgress = 0.67f),
            BandModel(value = 100, expectedStatus = MarketIndexStatus.Info, expectedProgress = 1f),
            // a reading off the scale keeps its number but its band and gauge stay inside the track
            BandModel(value = 105, expectedStatus = MarketIndexStatus.Info, expectedProgress = 1f),
            BandModel(value = -5, expectedStatus = MarketIndexStatus.Error, expectedProgress = 0f),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ChangeDirection {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: DirectionModel) {
            // Arrange
            val indicators = indicatorsOf(marketCapChange24h = model.change?.let(::BigDecimal))

            // Act
            val actual = converter.convert(indicators) as TotalMarketCapUM.Content

            // Assert
            assertThat(actual.change.direction).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            DirectionModel(change = "0.0161", expected = TangemPriceChange.Direction.Up),
            DirectionModel(change = "-0.0161", expected = TangemPriceChange.Direction.Down),
            DirectionModel(change = "0", expected = TangemPriceChange.Direction.Neutral),
            DirectionModel(change = null, expected = TangemPriceChange.Direction.Neutral),
            // a change that rounds away at the rendered precision must not get an arrow either
            DirectionModel(change = "0.0000004", expected = TangemPriceChange.Direction.Neutral),
            DirectionModel(change = "0.00006", expected = TangemPriceChange.Direction.Up),
        )
    }

    internal data class MissingCapitalisationModel(val totalMarketCap: BigDecimal?)

    internal data class BandModel(
        val value: Int,
        val expectedStatus: MarketIndexStatus,
        val expectedProgress: Float,
    )

    internal data class DirectionModel(val change: String?, val expected: TangemPriceChange.Direction)

    /** Below `1` so `compact()` stays on the JDK formatter — see the class KDoc */
    private fun indicatorsOf(
        totalMarketCap: BigDecimal? = BigDecimal("0.5"),
        marketCapChange24h: BigDecimal? = BigDecimal("-0.0093"),
        fearGreed: MarketIndicators.Index? = null,
        altcoinSeason: MarketIndicators.Index? = null,
    ) = MarketIndicators(
        totalMarketCap = totalMarketCap,
        marketCapChange24h = marketCapChange24h,
        fearGreed = fearGreed,
        altcoinSeason = altcoinSeason,
    )
}