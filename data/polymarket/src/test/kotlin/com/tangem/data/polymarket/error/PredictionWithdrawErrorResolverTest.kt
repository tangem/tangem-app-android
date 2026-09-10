package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionWithdrawErrorResolverTest {

    private val resolver = PredictionWithdrawErrorResolver(moshi = Moshi.Builder().build())

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
        TestModel(http(code = 400, detail = "data is not a transfer"), PredictionWithdrawError.InvalidRequest("data is not a transfer")),
        TestModel(http(code = 401), PredictionWithdrawError.Unauthorized),
        TestModel(http(code = 404), PredictionWithdrawError.NotFound),
        TestModel(http(code = 409, detail = "nonce taken"), PredictionWithdrawError.NonceTaken("nonce taken")),
        TestModel(http(code = 422, detail = "invalid signature"), PredictionWithdrawError.RelayerRejected("invalid signature")),
        TestModel(http(code = 502), PredictionWithdrawError.RelayerUnavailable),
        TestModel(http(code = 418, detail = "teapot"), PredictionWithdrawError.Unexpected(httpCode = 418, detail = "teapot")),
    )

    private fun http(code: Int, detail: String? = null): ApiResponseError.HttpException = ApiResponseError.HttpException(
        code = ApiResponseError.HttpException.Code.entries.first { it.numericCode == code },
        message = "",
        errorBody = detail?.let { """{"detail":"$it"}""" },
    )
}