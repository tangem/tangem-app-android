package com.tangem.data.onramp.converters.error

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.grow.datasource.express.models.response.ExpressErrorResponse
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class OnrampErrorConverterTest {

    private val converter = OnrampErrorConverter(
        legacyDataStoreMoshi.adapter(ExpressErrorResponse::class.java),
    )

    @Test
    fun `GIVEN too small response WHEN convert THEN required amount is scaled`() {
        // Act
        val actual = converter.convert(
            """{"error":{"code":2250,"description":"too small","value":{"minAmount":"1234","decimals":2}}}""",
        )

        // Assert
        assertThat(actual).isEqualTo(OnrampError.AmountError.TooSmallError(BigDecimal("12.34")))
    }

    @Test
    fun `GIVEN too big response WHEN convert THEN required amount is scaled`() {
        // Act
        val actual = converter.convert(
            """{"error":{"code":2251,"description":"too big","value":{"maxAmount":"123456","decimals":3}}}""",
        )

        // Assert
        assertThat(actual).isEqualTo(OnrampError.AmountError.TooBigError(BigDecimal("123.456")))
    }

    @Test
    fun `GIVEN generic express error WHEN convert THEN data error preserves code and description`() {
        // Act
        val actual = converter.convert(
            """{"error":{"code":4001,"description":"declined","value":null}}""",
        )

        // Assert
        assertThat(actual).isEqualTo(OnrampError.DataError(code = "4001", description = "declined"))
    }

    @Test
    fun `GIVEN malformed response WHEN convert THEN domain error contains parser message`() {
        // Act
        val actual = converter.convert("not-json")

        // Assert
        assertThat(actual).isInstanceOf(OnrampError.DomainError::class.java)
    }

    @Test
    fun `GIVEN amount error without valid bounds WHEN convert THEN data error is returned`() {
        // Act
        val actual = converter.convert(
            """{"error":{"code":2250,"description":"missing","value":{"minAmount":"x","decimals":2}}}""",
        )

        // Assert
        assertThat(actual).isEqualTo(OnrampError.DataError(code = "2250", description = "missing"))
    }
}