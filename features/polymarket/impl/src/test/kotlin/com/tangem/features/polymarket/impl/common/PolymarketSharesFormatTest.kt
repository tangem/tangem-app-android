package com.tangem.features.polymarket.impl.common

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSharesFormatTest {

    private val defaultLocale = Locale.getDefault()

    @BeforeAll
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @AfterAll
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN shares WHEN formatted THEN a position is never rounded up and a trade shows two decimals`(
        model: SharesFormatModel,
    ) {
        // Arrange
        val shares = BigDecimal(model.shares)

        // Act
        val position = shares.formatPolymarketPositionShares()
        val trade = shares.formatPolymarketShares()

        // Assert
        assertThat(position).isEqualTo(model.expectedPosition)
        assertThat(trade).isEqualTo(model.expectedTrade)
    }

    private fun provideTestModels(): List<SharesFormatModel> = listOf(
        SharesFormatModel(shares = "15.1663", expectedPosition = "15.1663", expectedTrade = "15.17"),
        SharesFormatModel(shares = "51.2345678", expectedPosition = "51.234567", expectedTrade = "51.23"),
        SharesFormatModel(shares = "0.0063", expectedPosition = "0.0063", expectedTrade = "0.01"),
        SharesFormatModel(shares = "15.160000", expectedPosition = "15.16", expectedTrade = "15.16"),
        SharesFormatModel(shares = "4612.54", expectedPosition = "4,612.54", expectedTrade = "4,612.54"),
        SharesFormatModel(shares = "51", expectedPosition = "51", expectedTrade = "51"),
    )

    data class SharesFormatModel(val shares: String, val expectedPosition: String, val expectedTrade: String)
}