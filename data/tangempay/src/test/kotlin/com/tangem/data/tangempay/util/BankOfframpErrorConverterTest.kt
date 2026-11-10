package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankOfframpErrorConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN named bank off-ramp error WHEN converted THEN its own error keeping the universal code`(
        model: NamedErrorModel,
    ) {
        // Act
        val actual = BankOfframpErrorConverter.convert(
            BankOfframpErrorConverter.Input(error = model.error, operation = "createQuote"),
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        assertThat(actual.errorCode).isEqualTo(model.expectedCode)
    }

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

    private fun provideTestModels() = listOf(
        NamedErrorModel(
            error = TangemPayApiError.BankOfframpQuoteExpired,
            expected = BankOfframpError.QuoteExpired,
            expectedCode = 104140164,
        ),
        NamedErrorModel(
            error = TangemPayApiError.BankOfframpRegionUnsupported,
            expected = BankOfframpError.SenderRegionUnsupported,
            expectedCode = 104140165,
        ),
        NamedErrorModel(
            error = TangemPayApiError.BankOfframpVerificationIncomplete,
            expected = BankOfframpError.VerificationIncomplete,
            expectedCode = 104140166,
        ),
        NamedErrorModel(
            error = TangemPayApiError.BankOfframpConflict,
            expected = BankOfframpError.Conflict,
            expectedCode = 104140167,
        ),
        NamedErrorModel(
            error = TangemPayApiError.BankOfframpAmountOutOfLimits,
            expected = BankOfframpError.AmountOutOfLimits,
            expectedCode = 104140168,
        ),
        NamedErrorModel(
            error = TangemPayApiError.BankOfframpRejected,
            expected = BankOfframpError.Rejected,
            expectedCode = 104140169,
        ),
    )

    data class NamedErrorModel(val error: TangemPayApiError, val expected: BankOfframpError, val expectedCode: Int)
}