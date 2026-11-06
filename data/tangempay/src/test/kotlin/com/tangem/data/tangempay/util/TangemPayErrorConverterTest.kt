package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.tangempay.error.TangemPayApiError
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayErrorConverterTest {

    private val converter = TangemPayErrorConverter(json = NetworkJsonFactory.create())

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
            expected = TangemPayApiError.UnknownWithoutCode,
        ),
        ConvertModel(
            name = "5xx wins over the body code -> ServerUnavailable",
            throwable = httpException(Code.INTERNAL_SERVER_ERROR, body = coded(CARD_ISSUE_INSUFFICIENT_BALANCE)),
            expected = TangemPayApiError.ServerUnavailable,
        ),
        ConvertModel(
            name = "404 wins over the body code -> NotFound",
            throwable = httpException(Code.NOT_FOUND, body = coded(CARD_ISSUE_INSUFFICIENT_BALANCE)),
            expected = TangemPayApiError.NotFound,
        ),
        ConvertModel(
            name = "401 wins over the body code -> RefreshTokenExpired",
            throwable = httpException(Code.UNAUTHORIZED, body = coded(CARD_ISSUE_INSUFFICIENT_BALANCE)),
            expected = TangemPayApiError.RefreshTokenExpired,
        ),
        ConvertModel(
            name = "409 with the active-order code -> CardIssueActiveOrderExists",
            throwable = httpException(Code.CONFLICT, body = coded(CARD_ISSUE_ACTIVE_ORDER_EXISTS)),
            expected = TangemPayApiError.CardIssueActiveOrderExists,
        ),
        ConvertModel(
            name = "400 with the offer code -> CardIssueOfferNotAvailable",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_ISSUE_OFFER_NOT_AVAILABLE)),
            expected = TangemPayApiError.CardIssueOfferNotAvailable,
        ),
        ConvertModel(
            name = "400 with the balance code -> CardIssueInsufficientBalance",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_ISSUE_INSUFFICIENT_BALANCE)),
            expected = TangemPayApiError.CardIssueInsufficientBalance,
        ),
        ConvertModel(
            name = "400 with the shipping-address code -> CardIssueInvalidShippingAddress",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_ISSUE_INVALID_SHIPPING_ADDRESS)),
            expected = TangemPayApiError.CardIssueInvalidShippingAddress,
        ),
        ConvertModel(
            name = "400 with the issue emboss-name code -> CardIssueInvalidEmbossName",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_ISSUE_INVALID_EMBOSS_NAME)),
            expected = TangemPayApiError.CardIssueInvalidEmbossName,
        ),
        ConvertModel(
            name = "400 with an unmodelled code -> Unknown",
            throwable = httpException(Code.BAD_REQUEST, body = coded(UNMODELLED_CODE)),
            expected = TangemPayApiError.Unknown(errorCode = FEATURE_CODE + UNMODELLED_CODE),
        ),
        ConvertModel(
            name = "400 with the activation invalid-card-data code -> CardActivationInvalidCardData",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_ACTIVATION_INVALID_CARD_DATA)),
            expected = TangemPayApiError.CardActivationInvalidCardData,
        ),
        ConvertModel(
            name = "400 with the not-physical code -> CardActivationCardNotPhysical",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_ACTIVATION_CARD_NOT_PHYSICAL)),
            expected = TangemPayApiError.CardActivationCardNotPhysical,
        ),
        ConvertModel(
            name = "400 with the already-active code -> CardActivationCardAlreadyActive",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_ACTIVATION_CARD_ALREADY_ACTIVE)),
            expected = TangemPayApiError.CardActivationCardAlreadyActive,
        ),
        ConvertModel(
            name = "400 with the not-ready code -> CardActivationCardNotReadyForActivation",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_ACTIVATION_CARD_NOT_READY)),
            expected = TangemPayApiError.CardActivationCardNotReadyForActivation,
        ),
        ConvertModel(
            name = "409 with the active-order code -> CardActivationActiveOrderExists",
            throwable = httpException(Code.CONFLICT, body = coded(CARD_ACTIVATION_ACTIVE_ORDER_EXISTS)),
            expected = TangemPayApiError.CardActivationActiveOrderExists,
        ),
        ConvertModel(
            name = "400 with the real activation body shape -> mapped by code, extra fields ignored",
            throwable = httpException(
                Code.BAD_REQUEST,
                body = """{"error":{"code":$CARD_ACTIVATION_INVALID_CARD_DATA,""" +
                    """"name":"CardActivationInvalidCardDataException","type":"validation",""" +
                    """"correlationId":"123"},"result":null}""",
            ),
            expected = TangemPayApiError.CardActivationInvalidCardData,
        ),
        ConvertModel(
            name = "400 with the reissue invalid-source-card code -> CardReissuePlasticInvalidSourceCard",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_REISSUE_PLASTIC_INVALID_SOURCE_CARD)),
            expected = TangemPayApiError.CardReissuePlasticInvalidSourceCard,
        ),
        ConvertModel(
            name = "409 with the reissue active-order code -> CardReissuePlasticActiveOrderExists",
            throwable = httpException(Code.CONFLICT, body = coded(CARD_REISSUE_PLASTIC_ACTIVE_ORDER_EXISTS)),
            expected = TangemPayApiError.CardReissuePlasticActiveOrderExists,
        ),
        ConvertModel(
            name = "400 with the reissue balance code -> CardReissuePlasticInsufficientBalance",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_REISSUE_PLASTIC_INSUFFICIENT_BALANCE)),
            expected = TangemPayApiError.CardReissuePlasticInsufficientBalance,
        ),
        ConvertModel(
            name = "400 with the reissue not-available code -> CardReissuePlasticNotAvailable",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_REISSUE_PLASTIC_NOT_AVAILABLE)),
            expected = TangemPayApiError.CardReissuePlasticNotAvailable,
        ),
        ConvertModel(
            name = "400 with the reissue shipping-address code -> CardReissuePlasticInvalidShippingAddress",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_REISSUE_PLASTIC_INVALID_SHIPPING_ADDRESS)),
            expected = TangemPayApiError.CardReissuePlasticInvalidShippingAddress,
        ),
        ConvertModel(
            name = "400 with the reissue emboss-name code -> CardReissuePlasticInvalidEmbossName",
            throwable = httpException(Code.BAD_REQUEST, body = coded(CARD_REISSUE_PLASTIC_INVALID_EMBOSS_NAME)),
            expected = TangemPayApiError.CardReissuePlasticInvalidEmbossName,
        ),
        ConvertModel(
            name = "400 with the real reissue body shape -> mapped by code, error name ignored",
            throwable = httpException(
                Code.BAD_REQUEST,
                body = """{"error":{"code":$CARD_REISSUE_PLASTIC_INVALID_SOURCE_CARD,""" +
                    """"name":"CardReissuePlasticInvalidSourceCardException","type":"validation",""" +
                    """"correlationId":"123"},"result":null}""",
            ),
            expected = TangemPayApiError.CardReissuePlasticInvalidSourceCard,
        ),
        ConvertModel(
            name = "400 without a body -> UnknownWithoutCode",
            throwable = httpException(Code.BAD_REQUEST, body = null),
            expected = TangemPayApiError.UnknownWithoutCode,
        ),
        ConvertModel(
            name = "400 with an unparseable body -> UnknownWithoutCode",
            throwable = httpException(Code.BAD_REQUEST, body = "}not json{"),
            expected = TangemPayApiError.UnknownWithoutCode,
        ),
    )

    internal data class ConvertModel(
        val name: String,
        val throwable: Throwable,
        val expected: TangemPayApiError,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val FEATURE_CODE = 104_000_000
        const val CARD_ISSUE_ACTIVE_ORDER_EXISTS = 140114
        const val CARD_ISSUE_OFFER_NOT_AVAILABLE = 140115
        const val CARD_ISSUE_INSUFFICIENT_BALANCE = 140116
        const val CARD_ISSUE_INVALID_SHIPPING_ADDRESS = 140126
        const val CARD_ACTIVATION_INVALID_CARD_DATA = 140127
        const val CARD_ACTIVATION_CARD_NOT_PHYSICAL = 140128
        const val CARD_ACTIVATION_CARD_ALREADY_ACTIVE = 140129
        const val CARD_ACTIVATION_CARD_NOT_READY = 140130
        const val CARD_ACTIVATION_ACTIVE_ORDER_EXISTS = 140131
        const val CARD_REISSUE_PLASTIC_INVALID_SOURCE_CARD = 140132
        const val CARD_REISSUE_PLASTIC_ACTIVE_ORDER_EXISTS = 140133
        const val CARD_REISSUE_PLASTIC_INSUFFICIENT_BALANCE = 140134
        const val CARD_REISSUE_PLASTIC_NOT_AVAILABLE = 140135
        const val CARD_REISSUE_PLASTIC_INVALID_SHIPPING_ADDRESS = 140136
        const val CARD_REISSUE_PLASTIC_INVALID_EMBOSS_NAME = 140143
        const val CARD_ISSUE_INVALID_EMBOSS_NAME = 140144
        const val UNMODELLED_CODE = 149999

        fun httpException(code: Code, body: String?) = ApiResponseError.HttpException(
            code = code,
            message = null,
            errorBody = body,
        )

        fun coded(code: Int) = """{"error":{"code":$code}}"""
    }
}