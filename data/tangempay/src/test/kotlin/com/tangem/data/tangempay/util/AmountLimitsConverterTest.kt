package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.spend.datasource.tangempay.models.response.AmountLimitsResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AmountLimitsConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN amount limits WHEN convert THEN mapped only with a minimum and a currency`(model: LimitsModel) {
        // Act
        val actual = AmountLimitsConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        LimitsModel(
            response = AmountLimitsResponse(min = BigDecimal("10"), max = null, currency = "USD"),
            expected = AmountLimits(min = BigDecimal("10"), max = null, currency = "USD"),
        ),
        LimitsModel(
            response = AmountLimitsResponse(min = BigDecimal("1"), max = BigDecimal("10000"), currency = "USD"),
            expected = AmountLimits(min = BigDecimal("1"), max = BigDecimal("10000"), currency = "USD"),
        ),
        LimitsModel(
            response = AmountLimitsResponse(min = null, max = BigDecimal("10000"), currency = "USD"),
            expected = null,
        ),
        LimitsModel(
            response = AmountLimitsResponse(min = BigDecimal("10"), max = null, currency = " "),
            expected = null,
        ),
        LimitsModel(
            response = AmountLimitsResponse(min = BigDecimal("10"), max = null, currency = null),
            expected = null,
        ),
    )

    data class LimitsModel(val response: AmountLimitsResponse, val expected: AmountLimits?)
}