package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.error.TangemPayApiError
import org.junit.jupiter.api.Test

internal class BankOfframpErrorConverterTest {

    @Test
    fun `GIVEN unsupported configuration code WHEN converted THEN unsupported configuration error`() {
        // Act
        val actual = BankOfframpErrorConverter.convert(
            BankOfframpErrorConverter.Input(
                error = TangemPayApiError.BankOfframpUnsupportedConfiguration,
                operation = "createOrder",
            ),
        )

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.UnsupportedConfiguration)
    }

    @Test
    fun `GIVEN any other api error WHEN converted THEN unknown error with the cause and the error code`() {
        // Act
        val actual = BankOfframpErrorConverter.convert(
            BankOfframpErrorConverter.Input(error = TangemPayApiError.CustomerIsBlocked, operation = "cancelOrder"),
        )

        // Assert
        val unknown = actual as BankOfframpError.Unknown
        assertThat(unknown.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(unknown.cause).hasMessageThat().isEqualTo(
            "Bank off-ramp cancelOrder failed: ${TangemPayApiError.CustomerIsBlocked}",
        )
        assertThat(unknown.isOutcomeUnknown).isFalse()
        assertThat(unknown.errorCode).isEqualTo(TangemPayApiError.CustomerIsBlocked.errorCode)
    }

    @Test
    fun `GIVEN an outcome unknown api error WHEN converted THEN unknown error marked as outcome unknown`() {
        // Act
        val actual = BankOfframpErrorConverter.convert(
            BankOfframpErrorConverter.Input(error = TangemPayApiError.ServerUnavailable, operation = "createOrder"),
        )

        // Assert
        assertThat((actual as BankOfframpError.Unknown).isOutcomeUnknown).isTrue()
    }
}