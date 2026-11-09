package com.tangem.features.earn.api.overview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnOverviewsTest {

    private val ethId = CryptoCurrency.ID.fromValue("coin⟨ETH⟩ethereum")
    private val unknownId = CryptoCurrency.ID.fromValue("coin⟨BCH⟩bitcoin-cash")

    @Test
    fun `GIVEN overview for currency WHEN get by its id THEN overview is returned`() {
        // Arrange
        val overview = createOverview()
        val overviews = createOverviews(byCurrency = mapOf(ethId to overview))

        // Act
        val actual = overviews[ethId]

        // Assert
        assertThat(actual).isEqualTo(overview)
    }

    @Test
    fun `GIVEN overview for currency WHEN get by unknown id THEN null is returned`() {
        // Arrange
        val overviews = createOverviews(byCurrency = mapOf(ethId to createOverview()))

        // Act
        val actual = overviews[unknownId]

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN empty overviews WHEN get by any id THEN null is returned`() {
        // Act
        val actual = EarnOverviews.Empty[ethId]

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN status source WHEN isComplete is read THEN it is true only for actual`(model: IsCompleteModel) {
        // Arrange
        val overviews = createOverviews(source = model.source)

        // Act
        val actual = overviews.isComplete

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN Empty constant WHEN read THEN map is empty and source is actual`() {
        // Act
        val actual = EarnOverviews.Empty

        // Assert
        assertThat(actual).isEqualTo(EarnOverviews(byCurrency = emptyMap(), source = StatusSource.ACTUAL))
    }

    @Test
    fun `GIVEN Empty constant WHEN isComplete is read THEN it is true`() {
        // Act
        val actual = EarnOverviews.Empty.isComplete

        // Assert
        assertThat(actual).isTrue()
    }

    private fun provideTestModels() = listOf(
        IsCompleteModel(source = StatusSource.ACTUAL, expected = true),
        IsCompleteModel(source = StatusSource.CACHE, expected = false),
        IsCompleteModel(source = StatusSource.ONLY_CACHE, expected = false),
    )

    private fun createOverviews(
        byCurrency: Map<CryptoCurrency.ID, EarnOverview> = emptyMap(),
        source: StatusSource = StatusSource.ACTUAL,
    ) = EarnOverviews(byCurrency = byCurrency, source = source)

    private fun createOverview(
        headline: EarnHeadline? = EarnHeadline(
            rate = EarnProductRate(fraction = BigDecimal("0.05"), type = EarnRateType.APY),
            isActive = true,
            badge = stringReference("5% APY"),
            badgeAccent = EarnBadgeAccent.Success,
            route = null,
        ),
        banner: EarnBannerOverview? = EarnBannerOverview(
            rate = EarnProductRate(fraction = BigDecimal("0.05"), type = EarnRateType.APY),
            activeCount = 1,
            totalPosition = BigDecimal("1.5"),
        ),
    ) = EarnOverview(headline = headline, banner = banner)

    internal data class IsCompleteModel(val source: StatusSource, val expected: Boolean) {
        override fun toString(): String = "$source -> $expected"
    }
}