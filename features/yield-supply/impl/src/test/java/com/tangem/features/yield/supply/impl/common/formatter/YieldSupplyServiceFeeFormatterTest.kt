package com.tangem.features.yield.supply.impl.common.formatter

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyServiceFeeFormatterTest {

    private var originalLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun formatServiceFeeRate(model: RateModel) {
        // Act
        val actual = model.bps.formatServiceFeeRate()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class RateModel(val bps: Int, val expected: String)

    private fun provideTestModels() = listOf(
        RateModel(bps = 1500, expected = "15%"),
        RateModel(bps = 1550, expected = "15.5%"),
        RateModel(bps = 1_555, expected = "15.55%"),
        RateModel(bps = 10_000, expected = "100%"),
        RateModel(bps = 0, expected = "0%"),
    )
}