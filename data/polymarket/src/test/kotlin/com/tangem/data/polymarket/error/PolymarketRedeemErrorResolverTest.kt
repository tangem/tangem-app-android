package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketRedeemErrorResolverTest {

    private val resolver = PolymarketRedeemErrorResolver(ProblemDetailParser(moshi = Moshi.Builder().build()))

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: TestModel) {
        // Act
        val actual = resolver.resolve(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class TestModel(val error: ApiResponseError, val expected: PolymarketRedeemError)

    private fun provideTestModels() = listOf(
        TestModel(
            error = http(code = Code.BAD_REQUEST, detail = "Invalid conditionId: 0x00"),
            expected = PolymarketRedeemError.InvalidRequest("Invalid conditionId: 0x00"),
        ),
        TestModel(http(code = Code.UNAUTHORIZED), PolymarketRedeemError.Unauthorized),
        TestModel(http(code = Code.NOT_FOUND), PolymarketRedeemError.NotFound),
        TestModel(
            error = http(code = Code.UNPROCESSABLE_ENTITY, detail = "position is not redeemable"),
            expected = PolymarketRedeemError.RelayerRejected("position is not redeemable"),
        ),
        TestModel(http(code = Code.BAD_GATEWAY), PolymarketRedeemError.RelayerUnavailable),
        TestModel(
            error = http(code = Code.IM_A_TEAPOT, detail = "teapot"),
            expected = PolymarketRedeemError.Unexpected(httpCode = Code.IM_A_TEAPOT.numericCode, detail = "teapot"),
        ),
        TestModel(ApiResponseError.NetworkException(), PolymarketRedeemError.Network),
        TestModel(ApiResponseError.TimeoutException(), PolymarketRedeemError.Network),
        TestModel(
            error = ApiResponseError.UnknownException(cause = IllegalStateException("moshi blew up")),
            expected = PolymarketRedeemError.Unexpected(httpCode = null, detail = "moshi blew up"),
        ),
    )

    private fun http(code: Code, detail: String? = null): ApiResponseError.HttpException =
        ApiResponseError.HttpException(
            code = code,
            message = "",
            errorBody = detail?.let { """{"detail":"$it"}""" },
        )
}