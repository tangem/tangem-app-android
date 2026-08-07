package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.visa.error.VisaApiError
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayErrorConverterTest {

    private val converter = TangemPayErrorConverter(moshi = Moshi.Builder().build())

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun convert(model: ConvertModel) {
        // Act
        val actual = converter.convert(model.throwable)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "not an HttpException -> UnknownWithoutCode",
            throwable = IllegalStateException("boom"),
            expected = VisaApiError.UnknownWithoutCode,
        ),
        ConvertModel(
            name = "5xx wins over everything -> ServerUnavailable",
            throwable = httpException(Code.INTERNAL_SERVER_ERROR, body = named("INSUFFICIENT_FUNDS")),
            expected = VisaApiError.ServerUnavailable,
        ),
        ConvertModel(
            name = "401 wins over a named error -> RefreshTokenExpired",
            throwable = httpException(Code.UNAUTHORIZED, body = named("PLASTIC_NOT_AVAILABLE")),
            expected = VisaApiError.RefreshTokenExpired,
        ),
        ConvertModel(
            name = "404 without a body -> NotFound",
            throwable = httpException(Code.NOT_FOUND, body = null),
            expected = VisaApiError.NotFound,
        ),
        ConvertModel(
            name = "404 with an unparseable body -> NotFound",
            throwable = httpException(Code.NOT_FOUND, body = "}not json{"),
            expected = VisaApiError.NotFound,
        ),
        ConvertModel(
            name = "404 with an unmodelled name -> NotFound",
            throwable = httpException(Code.NOT_FOUND, body = named("SOMETHING_NEW")),
            expected = VisaApiError.NotFound,
        ),
        ConvertModel(
            name = "404 with a numeric code but no name still short-circuits -> NotFound",
            throwable = httpException(Code.NOT_FOUND, body = """{"error":{"code":140116}}"""),
            expected = VisaApiError.NotFound,
        ),
        ConvertModel(
            name = "404 with a modelled name -> the named error",
            throwable = httpException(Code.NOT_FOUND, body = named("PLASTIC_NOT_AVAILABLE")),
            expected = VisaApiError.PlasticNotAvailable,
        ),
        ConvertModel(
            name = "409 ALREADY_HAS_ACTIVE_ORDER -> AlreadyHasActiveOrder",
            throwable = httpException(Code.CONFLICT, body = named("ALREADY_HAS_ACTIVE_ORDER")),
            expected = VisaApiError.AlreadyHasActiveOrder,
        ),
        ConvertModel(
            name = "400 INSUFFICIENT_FUNDS -> InsufficientFunds",
            throwable = httpException(Code.BAD_REQUEST, body = named("INSUFFICIENT_FUNDS")),
            expected = VisaApiError.InsufficientFunds,
        ),
        ConvertModel(
            name = "400 INVALID_SHIPPING_ADDRESS -> InvalidShippingAddress",
            throwable = httpException(Code.BAD_REQUEST, body = named("INVALID_SHIPPING_ADDRESS")),
            expected = VisaApiError.InvalidShippingAddress,
        ),
        ConvertModel(
            name = "400 COUNTRY_NOT_SUPPORTED -> CountryNotSupported",
            throwable = httpException(Code.BAD_REQUEST, body = named("COUNTRY_NOT_SUPPORTED")),
            expected = VisaApiError.CountryNotSupported,
        ),
        ConvertModel(
            name = "400 OFFER_NOT_AVAILABLE -> OfferNotAvailable",
            throwable = httpException(Code.BAD_REQUEST, body = named("OFFER_NOT_AVAILABLE")),
            expected = VisaApiError.OfferNotAvailable,
        ),
        ConvertModel(
            name = "numeric mapping still applies when no name is present",
            throwable = httpException(Code.BAD_REQUEST, body = """{"error":{"code":140116}}"""),
            expected = VisaApiError.CardIssueInsufficientBalance,
        ),
        ConvertModel(
            name = "non-404 without a body -> UnknownWithoutCode",
            throwable = httpException(Code.BAD_REQUEST, body = null),
            expected = VisaApiError.UnknownWithoutCode,
        ),
        ConvertModel(
            name = "non-404 with an unparseable body -> UnknownWithoutCode",
            throwable = httpException(Code.BAD_REQUEST, body = "}not json{"),
            expected = VisaApiError.UnknownWithoutCode,
        ),
    )

    internal data class ConvertModel(
        val name: String,
        val throwable: Throwable,
        val expected: VisaApiError,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        fun httpException(code: Code, body: String?) = ApiResponseError.HttpException(
            code = code,
            message = null,
            errorBody = body,
        )

        fun named(name: String) = """{"error":{"code":0,"name":"$name"}}"""
    }
}