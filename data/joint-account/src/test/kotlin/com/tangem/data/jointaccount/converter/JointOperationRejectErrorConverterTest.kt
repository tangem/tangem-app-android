package com.tangem.data.jointaccount.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationRejectErrorConverterTest {

    private val converter = JointOperationRejectErrorConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = converter.convert(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<ConvertModel> {
        val serverError = httpException(Code.INTERNAL_SERVER_ERROR)

        return listOf(
            ConvertModel(error = httpException(Code.NOT_FOUND), expected = JointOperationRejectError.OperationNotFound),
            ConvertModel(error = httpException(Code.CONFLICT), expected = JointOperationRejectError.ThresholdReached),
            ConvertModel(error = httpException(Code.FORBIDDEN), expected = JointOperationRejectError.NotAMember),
            ConvertModel(error = serverError, expected = JointOperationRejectError.Failed(cause = serverError)),
        )
    }

    @Test
    fun `GIVEN a non-http failure WHEN convert THEN it stays a plain failure`() {
        // Arrange
        val timeout = ApiResponseError.TimeoutException()

        // Act
        val actual = converter.convert(timeout)

        // Assert
        assertThat(actual).isEqualTo(JointOperationRejectError.Failed(cause = timeout))
    }

    internal data class ConvertModel(val error: Throwable, val expected: JointOperationRejectError)

    private fun httpException(code: Code) = ApiResponseError.HttpException(code = code, message = null, errorBody = null)
}