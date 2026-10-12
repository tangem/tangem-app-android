package com.tangem.domain.visa.error

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class VisaApiErrorTest {

    @Test
    fun `GIVEN backend code 140141 WHEN mapped THEN unsupported configuration error`() {
        // Act
        val error = VisaApiError.fromBackendError(backendErrorCode = 140141)

        // Assert
        assertThat(error).isEqualTo(VisaApiError.BankOfframpUnsupportedConfiguration)
    }

    @Test
    fun `GIVEN an unmapped backend code WHEN mapped THEN unknown error with the universal code`() {
        // Act
        val error = VisaApiError.fromBackendError(backendErrorCode = 140199)

        // Assert
        assertThat(error).isEqualTo(VisaApiError.Unknown(errorCode = 104140199))
    }
}