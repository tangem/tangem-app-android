package com.tangem.features.earn.impl.overview

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnRateBadgeConverterTest {

    private val converter = EarnRateBadgeConverter()

    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun pinLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN rate type WHEN convert THEN badge text of that type with formatted percent`(model: ConvertModel) {
        // Act
        val actual = converter.convert(model.rate)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            rate = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APR),
            expected = resourceReference(R.string.staking_apr_earn_badge, wrappedList("5.50")),
        ),
        ConvertModel(
            rate = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APY),
            expected = resourceReference(R.string.yield_module_earn_badge, wrappedList("5.50")),
        ),
    )

    internal data class ConvertModel(val rate: EarnProductRate, val expected: TextReference) {
        override fun toString(): String = "${rate.type} ${rate.fraction}"
    }
}