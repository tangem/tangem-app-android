package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionWithdrawErrorResolverTest {

    private val resolver = PredictionWithdrawErrorResolver(ProblemDetailParser(moshi = Moshi.Builder().build()))

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: TestModel) {
        // Act
        val actual = resolver.resolve(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class TestModel(val error: ApiResponseError, val expected: PredictionWithdrawError)

    private fun provideTestModels() = listOf(
        TestModel(
            error = http(code = Code.BAD_REQUEST, detail = "data is not a transfer"),
            expected = PredictionWithdrawError.InvalidRequest("data is not a transfer"),
        ),
        TestModel(http(code = Code.UNAUTHORIZED), PredictionWithdrawError.Unauthorized),
        TestModel(http(code = Code.NOT_FOUND), PredictionWithdrawError.NotFound),
        TestModel(http(code = Code.CONFLICT, detail = "nonce taken"), PredictionWithdrawError.NonceTaken("nonce taken")),
        TestModel(
            error = http(code = Code.UNPROCESSABLE_ENTITY, detail = "invalid signature"),
            expected = PredictionWithdrawError.RelayerRejected("invalid signature"),
        ),
        TestModel(http(code = Code.BAD_GATEWAY), PredictionWithdrawError.RelayerUnavailable),
        TestModel(
            error = http(code = Code.IM_A_TEAPOT, detail = "teapot"),
            expected = PredictionWithdrawError.Unexpected(httpCode = Code.IM_A_TEAPOT.numericCode, detail = "teapot"),
        ),
        TestModel(ApiResponseError.NetworkException(), PredictionWithdrawError.Network),
        TestModel(ApiResponseError.TimeoutException(), PredictionWithdrawError.Network),
        TestModel(
            error = ApiResponseError.UnknownException(cause = IllegalStateException("moshi blew up")),
            expected = PredictionWithdrawError.Unexpected(httpCode = null, detail = "moshi blew up"),
        ),
    )

    private fun http(code: Code, detail: String? = null): ApiResponseError.HttpException =
        ApiResponseError.HttpException(
            code = code,
            message = "",
            errorBody = detail?.let { """{"detail":"$it"}""" },
        )
}