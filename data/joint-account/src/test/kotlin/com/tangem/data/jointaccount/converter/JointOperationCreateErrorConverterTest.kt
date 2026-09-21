package com.tangem.data.jointaccount.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.logging.TangemLogger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationCreateErrorConverterTest {

    private val converter = JointOperationCreateErrorConverter(moshi = Moshi.Builder().build())

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = converter.convert(httpException(code = model.code, errorBody = model.errorBody))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ConvertModel(
        val code: Code,
        val errorBody: String?,
        val expected: JointOperationCreateError,
    )

    private fun provideTestModels(): List<ConvertModel> {
        val plainBadRequest = httpException(code = Code.BAD_REQUEST, errorBody = errorBody(code = "INVALID_BASE_GAS"))
        val unparsableBadRequest = httpException(code = Code.BAD_REQUEST, errorBody = "NONCE_MISMATCH")
        val notFound = httpException(code = Code.NOT_FOUND, errorBody = null)

        return listOf(
            ConvertModel(
                code = Code.CONFLICT,
                errorBody = null,
                expected = JointOperationCreateError.SlotBusy,
            ),
            ConvertModel(
                code = Code.BAD_REQUEST,
                errorBody = errorBody(code = "NONCE_MISMATCH"),
                expected = JointOperationCreateError.NetworkStateMoved,
            ),
            ConvertModel(
                code = Code.BAD_REQUEST,
                errorBody = errorBody(code = "GAS_PRICE_TOO_LOW"),
                expected = JointOperationCreateError.NetworkStateMoved,
            ),
            ConvertModel(
                code = Code.BAD_REQUEST,
                errorBody = errorBody(code = "GAS_PRICE_TOO_HIGH"),
                expected = JointOperationCreateError.NetworkStateMoved,
            ),
            ConvertModel(
                code = plainBadRequest.code,
                errorBody = plainBadRequest.errorBody,
                expected = JointOperationCreateError.Failed(cause = plainBadRequest),
            ),
            // The code is a field of its own: a reason spelled out in prose is not one the client acts on
            ConvertModel(
                code = unparsableBadRequest.code,
                errorBody = unparsableBadRequest.errorBody,
                expected = JointOperationCreateError.Failed(cause = unparsableBadRequest),
            ),
            ConvertModel(
                code = notFound.code,
                errorBody = notFound.errorBody,
                expected = JointOperationCreateError.Failed(cause = notFound),
            ),
        )
    }

    /**
     * A moved nonce and a risen gas price share one error and one text by decision, so which of them the
     * backend reported is knowable afterwards only from the log.
     */
    @Test
    fun `GIVEN a moved network state WHEN convert THEN the reported marker is logged`() {
        // Arrange
        mockkObject(TangemLogger)
        val message = slot<String>()
        every { TangemLogger.e(capture(message), any()) } just Runs

        // Act
        converter.convert(httpException(code = Code.BAD_REQUEST, errorBody = errorBody(code = "GAS_PRICE_TOO_LOW")))

        // Assert
        assertThat(message.captured).contains("GAS_PRICE_TOO_LOW")
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN a non-http failure WHEN convert THEN it stays a plain failure`() {
        // Arrange
        val timeout = ApiResponseError.TimeoutException()

        // Act
        val actual = converter.convert(timeout)

        // Assert
        assertThat(actual).isEqualTo(JointOperationCreateError.Failed(cause = timeout))
    }

    private fun errorBody(code: String): String =
        """{"statusCode":400,"error":"Bad Request","message":"nonce 7 came, the Safe is at 8","code":"$code"}"""

    private fun httpException(code: Code, errorBody: String?) = ApiResponseError.HttpException(
        code = code,
        message = null,
        errorBody = errorBody,
    )
}