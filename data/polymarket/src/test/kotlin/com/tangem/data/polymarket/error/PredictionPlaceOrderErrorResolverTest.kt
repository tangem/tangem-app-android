package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionPlaceOrderErrorResolverTest {

    private val resolver = PredictionPlaceOrderErrorResolver()

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ResolveModel) {
        // Act
        val actual = resolver.resolve(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ResolveModel(
        val name: String,
        val error: ApiResponseError,
        val expected: PredictionPlaceOrderError,
    ) {
        override fun toString(): String = name
    }

    private val networkError = ApiResponseError.NetworkException()
    private val timeoutError = ApiResponseError.TimeoutException()

    private fun provideTestModels() = listOf(
        ResolveModel(
            name = "a transport failure leaves the outcome unknown, never failed",
            error = networkError,
            expected = PredictionPlaceOrderError.Ambiguous(detail = networkError.message),
        ),
        ResolveModel(
            name = "a timeout is equally unknown: the order may have reached the book",
            error = timeoutError,
            expected = PredictionPlaceOrderError.Ambiguous(detail = timeoutError.message),
        ),
        ResolveModel(
            name = "401 is about our credentials, not about the order",
            error = ApiResponseError.HttpException(code = Code.UNAUTHORIZED, message = null, errorBody = "bad key"),
            expected = PredictionPlaceOrderError.Unauthorized(detail = "bad key"),
        ),
        ResolveModel(
            name = "another 4xx is the exchange refusing this order",
            error = ApiResponseError.HttpException(code = Code.BAD_REQUEST, message = null, errorBody = "min size"),
            expected = PredictionPlaceOrderError.Rejected(detail = "min size"),
        ),
        ResolveModel(
            name = "a fill-and-kill order that found no match is a market without liquidity",
            error = ApiResponseError.HttpException(code = Code.BAD_REQUEST, message = null, errorBody = FAK_NO_MATCH),
            expected = PredictionPlaceOrderError.NoLiquidity(detail = FAK_NO_MATCH),
        ),
        ResolveModel(
            name = "the same body on a 401 is still about our credentials",
            error = ApiResponseError.HttpException(code = Code.UNAUTHORIZED, message = null, errorBody = FAK_NO_MATCH),
            expected = PredictionPlaceOrderError.Unauthorized(detail = FAK_NO_MATCH),
        ),
        ResolveModel(
            name = "the same body on a 5xx is no verdict, so the outcome stays unknown",
            error = ApiResponseError.HttpException(
                code = Code.INTERNAL_SERVER_ERROR,
                message = null,
                errorBody = FAK_NO_MATCH,
            ),
            expected = PredictionPlaceOrderError.Ambiguous(detail = FAK_NO_MATCH),
        ),
        ResolveModel(
            name = "a 5xx is no verdict on the order, so its outcome is unknown",
            error = ApiResponseError.HttpException(code = Code.INTERNAL_SERVER_ERROR, message = null, errorBody = "x"),
            expected = PredictionPlaceOrderError.Ambiguous(detail = "x"),
        ),
        ResolveModel(
            name = "an unclassifiable failure may still have reached the exchange",
            error = ApiResponseError.UnknownException(IOException("boom")),
            expected = PredictionPlaceOrderError.Ambiguous(detail = "boom"),
        ),
    )

    private companion object {
        const val FAK_NO_MATCH = "{\"error\":\"no orders found to match with FAK order. FAK orders are " +
            "partially filled or killed if no match is found.\",\"orderID\":\"0x310aecad\"}"
    }
}