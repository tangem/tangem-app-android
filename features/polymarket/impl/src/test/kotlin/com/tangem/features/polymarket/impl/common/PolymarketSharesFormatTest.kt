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
    fun `GIVEN shares WHEN formatted THEN cut down to two decimals and a remainder below a cent kept whole`(
        model: SharesFormatModel,
    ) {
        // Arrange
        val shares = BigDecimal(model.shares)

        // Act
        val actual = shares.formatPolymarketShares()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<SharesFormatModel> = listOf(
        SharesFormatModel(shares = "15.1663", expected = "15.16"),
        SharesFormatModel(shares = "5.3999", expected = "5.39"),
        SharesFormatModel(shares = "97.95918", expected = "97.95"),
        SharesFormatModel(shares = "15.160000", expected = "15.16"),
        SharesFormatModel(shares = "4612.54", expected = "4,612.54"),
        SharesFormatModel(shares = "51", expected = "51"),
        SharesFormatModel(shares = "0.01", expected = "0.01"),
        SharesFormatModel(shares = "0.0063", expected = "0.0063"),
        SharesFormatModel(shares = "0.0000001", expected = "0"),
        SharesFormatModel(shares = "0", expected = "0"),
    )

    data class SharesFormatModel(val shares: String, val expected: String)
}