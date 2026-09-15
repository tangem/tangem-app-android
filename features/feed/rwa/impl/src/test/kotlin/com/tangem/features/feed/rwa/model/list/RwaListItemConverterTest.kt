package com.tangem.features.feed.rwa.model.list

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenQuotesShort
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RwaListItemConverterTest {

    private val converter = RwaListItemConverter(
        currentTrendInterval = RwaInterval.H24,
        appCurrency = AppCurrency.Default,
        onItemClick = {},
    )

    @Test
    fun `GIVEN a restricted asset WHEN convert THEN its icon carries the status dot`() {
        // Arrange
        val token = createToken(isRestrictedForCountry = true)

        // Act
        val actual = converter.convert(token)

        // Assert
        assertThat(actual.row.icon.indicator()).isNotNull()
    }

    @Test
    fun `GIVEN an allowed asset WHEN convert THEN its icon carries no dot`() {
        // Arrange
        val token = createToken(isRestrictedForCountry = false)

        // Act
        val actual = converter.convert(token)

        // Assert
        assertThat(actual.row.icon.indicator()).isNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun update(model: UpdateModel) {
        // Arrange — update() rebuilds the row, so the flag has to be re-read from the new model rather
        // than carried over from the previous icon state
        val prev = createToken(isRestrictedForCountry = model.wasRestricted)
        val prevUI = converter.convert(prev)
        val new = createToken(isRestrictedForCountry = model.isRestricted, price = BigDecimal(2))

        // Act
        val actual = converter.update(prev = prev, prevUI = prevUI, new = new)

        // Assert
        assertThat(actual.row.icon.indicator() != null).isEqualTo(model.isRestricted)
    }

    internal data class UpdateModel(val wasRestricted: Boolean, val isRestricted: Boolean)

    private fun provideTestModels() = listOf(
        UpdateModel(wasRestricted = true, isRestricted = true),
        UpdateModel(wasRestricted = false, isRestricted = false),
        // the same asset comes back restricted under another filter — the dot has to appear
        UpdateModel(wasRestricted = false, isRestricted = true),
        // and disappear again when it comes back allowed
        UpdateModel(wasRestricted = true, isRestricted = false),
    )

    private fun TangemTokenIcon.UiState.indicator(): TangemTokenIcon.State.Indicator? =
        (this as TangemTokenIcon.UiState.Token).tokenState.indicator

    private fun createToken(isRestrictedForCountry: Boolean, price: BigDecimal = BigDecimal.ONE) = TokenMarket(
        id = CryptoCurrency.RawID("apple-xstock"),
        name = "Apple xStock",
        symbol = "AAPLX",
        marketRating = 1,
        marketCap = null,
        isUnderMarketCapLimit = false,
        tokenQuotesShort = TokenQuotesShort(
            currentPrice = price,
            h24ChangePercent = BigDecimal.ZERO,
            weekChangePercent = BigDecimal.ZERO,
            monthChangePercent = BigDecimal.ZERO,
        ),
        tokenCharts = TokenMarket.Charts(h24 = null, week = null, month = null),
        yieldRate = null,
        updateTimestamp = null,
        networks = null,
        isRestrictedForCountry = isRestrictedForCountry,
        imageHost = "https://img.tangem.org/",
    )
}