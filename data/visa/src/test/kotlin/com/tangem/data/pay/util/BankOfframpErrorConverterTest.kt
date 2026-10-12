package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.visa.error.VisaApiError
import org.junit.jupiter.api.Test

internal class BankOfframpErrorConverterTest {

    @Test
    fun `GIVEN unsupported configuration code WHEN converted THEN unsupported configuration error`() {
        // Act
        val actual = BankOfframpErrorConverter.convert(
            error = VisaApiError.BankOfframpUnsupportedConfiguration,
            operation = "createOrder",
        )

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.UnsupportedConfiguration)
    }

    @Test
    fun `GIVEN any other api error WHEN converted THEN unknown error with the cause`() {
        // Act
        val actual = BankOfframpErrorConverter.convert(
            error = VisaApiError.CustomerIsBlocked,
            operation = "cancelOrder",
        )

        // Assert
        val cause = (actual as BankOfframpError.Unknown).cause
        assertThat(cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(cause).hasMessageThat().isEqualTo(
            "Bank off-ramp cancelOrder failed: ${VisaApiError.CustomerIsBlocked}",
        )
    }
}