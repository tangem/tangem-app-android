package com.tangem.domain.transaction.error

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GaslessSendErrorMapperTest {

    @ParameterizedTest
    @ProvideTestModels
    fun mapToGaslessSendError(model: TestModel) {
        // Act
        val actual = model.throwable.mapToGaslessSendError()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class TestModel(val throwable: Throwable, val expected: SendTransactionError)

    private fun provideTestModels() = listOf(
        TestModel(
            throwable = GaslessServiceException(httpCode = 503, message = "Service Unavailable"),
            expected = SendTransactionError.NetworkError(message = "Service Unavailable", code = "503"),
        ),
        TestModel(
            throwable = GaslessServiceException(httpCode = null, message = "Gasless service is unreachable"),
            expected = SendTransactionError.NetworkError(message = "Gasless service is unreachable", code = null),
        ),
        TestModel(
            throwable = IllegalStateException("Uncompiled transaction data required"),
            expected = SendTransactionError.DataError(message = "Uncompiled transaction data required"),
        ),
    )
}