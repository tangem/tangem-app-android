package com.tangem.data.staking.converters.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolErrorDetailsDTO
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class P2PEthPoolErrorConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertFromErrorDetails {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN p2p error code WHEN convertFromErrorDetails THEN mapped to the expected staking error`(
            model: ErrorModel,
        ) {
            // Act
            val actual = P2PEthPoolErrorConverter.convertFromErrorDetails(details(model.code))

            // Assert
            assertThat(actual).isInstanceOf(model.errorType)
            when (actual) {
                is StakingError.UnknownError -> assertException(actual.exception, model)
                is StakingError.DataError -> assertException(actual.exception, model)
                else -> assertThat(actual).isEqualTo(StakingError.InvalidAmount(model.message))
            }
        }

        private fun assertException(exception: Throwable, model: ErrorModel) {
            assertThat(exception.javaClass).isEqualTo(model.exceptionType)
            assertThat(exception.message).isEqualTo(model.message)
        }

        private fun provideTestModels() = listOf(
            unknown(101111, "Missing Bearer token: msg"),
            unknown(101109, "Invalid Bearer token: msg"),
            unknown(101110, "Server authorization error: msg"),
            invalidAmount(100101, "msg"),
            data(127104, "No withdrawable balance: msg"),
            unknown(127105, "Gas amount too low: msg"),
            invalidAmount(127106, "Invalid delegator address: msg"),
            unknown(127107, "Gas price too low: msg"),
            unknown(127108, "Transaction simulation failed: msg"),
            data(127101, "Invalid vault: msg"),
            data(124108, "Invalid delegator: msg"),
            data(127100, "Unsupported network: msg"),
            unknown(999999, "999999: msg"),
        )
    }

    private fun details(code: Int) = P2PEthPoolErrorDetailsDTO(code = code, message = "msg", name = null, errors = null)

    private fun unknown(code: Int, message: String) = ErrorModel(
        code = code,
        errorType = StakingError.UnknownError::class.java,
        exceptionType = Exception::class.java,
        message = message,
    )

    private fun data(code: Int, message: String) = ErrorModel(
        code = code,
        errorType = StakingError.DataError::class.java,
        exceptionType = IllegalStateException::class.java,
        message = message,
    )

    private fun invalidAmount(code: Int, message: String) = ErrorModel(
        code = code,
        errorType = StakingError.InvalidAmount::class.java,
        exceptionType = null,
        message = message,
    )

    internal data class ErrorModel(
        val code: Int,
        val errorType: Class<out StakingError>,
        val exceptionType: Class<out Throwable>?,
        val message: String,
    )
}