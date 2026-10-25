package com.tangem.data.polymarket.error

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
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
        val actual = resolver.resolve(error = model.error, identity = model.identity)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ResolveModel(
        val name: String,
        val error: ApiResponseError,
        val expected: PredictionPlaceOrderError,
        val identity: PolymarketClobIdentity = OWNER,
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
        ResolveModel(
            name = "an invalid signature on a session order refuses the session key",
            error = ApiResponseError.HttpException(
                code = Code.BAD_REQUEST,
                message = null,
                errorBody = INVALID_SIGNATURE,
            ),
            identity = SESSION,
            expected = PredictionPlaceOrderError.SessionKeyRefused(httpCode = 400, reason = "invalid signature"),
        ),
        ResolveModel(
            name = "an unauthorized signer on a session order refuses the session key",
            error = ApiResponseError.HttpException(code = Code.BAD_REQUEST, message = null, errorBody = SIGNER_REFUSED),
            identity = SESSION,
            expected = PredictionPlaceOrderError.SessionKeyRefused(
                httpCode = 400,
                reason = "the order signer address has to be the address of the API KEY",
            ),
        ),
        ResolveModel(
            name = "a bare 403 on a session order is a geoblock, not a refusal of the key",
            error = ApiResponseError.HttpException(code = Code.FORBIDDEN, message = null, errorBody = GEOBLOCKED),
            identity = SESSION,
            expected = PredictionPlaceOrderError.Ambiguous(detail = GEOBLOCKED),
        ),
        ResolveModel(
            name = "a 403 naming the API key on a session order refuses the session key",
            error = ApiResponseError.HttpException(code = Code.FORBIDDEN, message = null, errorBody = API_KEY_REFUSED),
            identity = SESSION,
            expected = PredictionPlaceOrderError.SessionKeyRefused(httpCode = 403, reason = "api key not allowed"),
        ),
        ResolveModel(
            name = "any other 4xx naming the signer on a session order refuses the session key",
            error = ApiResponseError.HttpException(code = Code.CONFLICT, message = null, errorBody = SIGNER_REFUSED),
            identity = SESSION,
            expected = PredictionPlaceOrderError.SessionKeyRefused(
                httpCode = 409,
                reason = "the order signer address has to be the address of the API KEY",
            ),
        ),
        ResolveModel(
            name = "a 5xx naming the signature on a session order is no verdict on the key",
            error = ApiResponseError.HttpException(
                code = Code.INTERNAL_SERVER_ERROR,
                message = null,
                errorBody = INVALID_SIGNATURE,
            ),
            identity = SESSION,
            expected = PredictionPlaceOrderError.Ambiguous(detail = INVALID_SIGNATURE),
        ),
        ResolveModel(
            name = "a 401 on a session order stays about the session credentials",
            error = ApiResponseError.HttpException(code = Code.UNAUTHORIZED, message = null, errorBody = "bad key"),
            identity = SESSION,
            expected = PredictionPlaceOrderError.Unauthorized(detail = "bad key"),
        ),
        ResolveModel(
            name = "another 400 on a session order is the exchange refusing the order",
            error = ApiResponseError.HttpException(code = Code.BAD_REQUEST, message = null, errorBody = MIN_SIZE),
            identity = SESSION,
            expected = PredictionPlaceOrderError.Rejected(detail = MIN_SIZE),
        ),
        ResolveModel(
            name = "an invalid signature on an owner order is rejected as before",
            error = ApiResponseError.HttpException(
                code = Code.BAD_REQUEST,
                message = null,
                errorBody = INVALID_SIGNATURE,
            ),
            expected = PredictionPlaceOrderError.Rejected(detail = INVALID_SIGNATURE),
        ),
        ResolveModel(
            name = "a 403 on an owner order leaves the outcome unknown as before",
            error = ApiResponseError.HttpException(code = Code.FORBIDDEN, message = null, errorBody = "forbidden"),
            expected = PredictionPlaceOrderError.Ambiguous(detail = "forbidden"),
        ),
    )

    private companion object {
        const val INVALID_SIGNATURE = "{\"error\":\"invalid signature\"}"
        const val SIGNER_REFUSED = "{\"error\":\"the order signer address has to be the address of the API KEY\"}"
        const val GEOBLOCKED = "{\"error\":\"Trading restricted in your region\"}"
        const val API_KEY_REFUSED = "{\"error\":\"api key not allowed\"}"
        const val MIN_SIZE = "{\"error\":\"invalid amount for a marketable BUY order, min size: 1\"}"
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "c2VjcmV0", passphrase = "pass")
        val OWNER = PolymarketClobIdentity.Owner(address = "0xowner", credentials = CREDENTIALS)
        val SESSION = PolymarketClobIdentity.Session(address = "0xsession", credentials = CREDENTIALS)
        const val FAK_NO_MATCH = "{\"error\":\"no orders found to match with FAK order. FAK orders are " +
            "partially filled or killed if no match is found.\",\"orderID\":\"0x310aecad\"}"
    }
}