package com.tangem.features.feed.rwa.model.list

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenQuotesShort
import com.tangem.domain.models.currency.CryptoCurrency
import org.junit.jupiter.api.Test
import java.math.BigDecimal

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

    @Test
    fun `GIVEN a restricted asset WHEN updated by a new quote THEN the dot survives`() {
        // Arrange — update() rebuilds the row from scratch, so the flag has to be re-read there too
        val prev = createToken(isRestrictedForCountry = true)
        val prevUI = converter.convert(prev)
        val new = createToken(isRestrictedForCountry = true, price = BigDecimal(2))

        // Act
        val actual = converter.update(prev = prev, prevUI = prevUI, new = new)

        // Assert
        assertThat(actual.row.icon.indicator()).isNotNull()
    }

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