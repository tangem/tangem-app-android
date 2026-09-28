package com.tangem.features.polymarket.impl.common

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.DASH_SIGN
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketPriceFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN price and market state WHEN formatted THEN rounds to tenths with the correct bounds`(case: PriceCase) {
        // Arrange
        val price = case.price?.toBigDecimal()

        // Act
        val actual = price.formatPolymarketPrice(
            isResolved = case.isResolved,
            withSymbol = case.withSymbol,
            locale = case.locale,
        )

        // Assert
        assertThat(actual).isEqualTo(case.expected)
    }

    private fun provideTestModels(): List<PriceCase> = listOf(
        // Raw price, expected unresolved price, expected resolved price.
        Triple("0", "0.1¢", "0¢"),
        Triple("0.0004", "0.1¢", "0¢"),
        Triple("0.00049", "0.1¢", "0¢"),
        Triple("0.0005", "0.1¢", "0.1¢"),
        Triple("0.001", "0.1¢", "0.1¢"),
        Triple("0.3559", "35.6¢", "35.6¢"),
        Triple("0.5", "50¢", "50¢"),
        Triple("0.7400", "74¢", "74¢"),
        Triple("0.7454", "74.5¢", "74.5¢"),
        Triple("0.7455", "74.6¢", "74.6¢"),
        Triple("0.746", "74.6¢", "74.6¢"),
        Triple("0.9994", "99.9¢", "99.9¢"),
        Triple("0.99949", "99.9¢", "99.9¢"),
        Triple("0.9995", "99.9¢", "100¢"),
        Triple("0.9996", "99.9¢", "100¢"),
        Triple("1", "99.9¢", "100¢"),
    ).flatMap { (price, unresolved, resolved) ->
        listOf(
            PriceCase(price = price, expected = unresolved),
            PriceCase(price = price, expected = resolved, isResolved = true),
        )
    } + listOf(
        PriceCase(price = null, expected = DASH_SIGN),
        PriceCase(price = null, expected = DASH_SIGN, isResolved = true),
        PriceCase(price = "0.7455", expected = "74.6", withSymbol = false),
        PriceCase(price = "0.9995", expected = "99.9", withSymbol = false),
        PriceCase(price = "0.7455", expected = "74,6¢", locale = Locale.forLanguageTag("ru-RU")),
        PriceCase(price = "0.74", expected = "74¢", locale = Locale.forLanguageTag("ru-RU")),
        PriceCase(price = "0.0004", expected = "0,1¢", locale = Locale.forLanguageTag("ru-RU")),
        PriceCase(price = "0.9995", expected = "99,9¢", locale = Locale.forLanguageTag("ru-RU")),
    )

    data class PriceCase(
        val price: String?,
        val expected: String,
        val isResolved: Boolean = false,
        val withSymbol: Boolean = true,
        val locale: Locale = Locale.US,
    )
}