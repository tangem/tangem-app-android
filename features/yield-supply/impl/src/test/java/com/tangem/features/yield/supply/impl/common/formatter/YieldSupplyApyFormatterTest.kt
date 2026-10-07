package com.tangem.features.yield.supply.impl.common.formatter

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyApyFormatterTest {

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
    fun formatApy(model: ApyModel) {
        // Act
        val actual = BigDecimal(model.raw).formatApy()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ApyModel(val raw: String, val expected: String)

    private fun provideTestModels() = listOf(
        ApyModel(raw = "3.09", expected = "3.09%"),
        ApyModel(raw = "3.0900", expected = "3.09%"),
        ApyModel(raw = "4.4949", expected = "4.49%"),
        ApyModel(raw = "1.4", expected = "1.40%"),
        ApyModel(raw = "12", expected = "12.00%"),
        ApyModel(raw = "0.000001", expected = "<0.01%"),
        ApyModel(raw = "0", expected = "0.00%"),
    )
}