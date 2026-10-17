package com.tangem.data.promo.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoRegistrationErrorConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = PromoRegistrationErrorConverter.convert(model.cause)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class ConvertModel(val cause: Throwable, val expected: PromoRegistrationError)

    private fun provideTestModels(): List<ConvertModel> {
        val badRequest = httpError(Code.BAD_REQUEST, errorBody = """{"title":"Main wallet not eligible"}""")
        val serverError = httpError(Code.INTERNAL_SERVER_ERROR)
        val networkError = ApiResponseError.NetworkException()
        val nonApiError = IllegalStateException("boom")

        return listOf(
            ConvertModel(httpError(Code.NOT_FOUND), PromoRegistrationError.CampaignNotFound),
            ConvertModel(httpError(Code.CONFLICT), PromoRegistrationError.CampaignNotActive),
        ) + listOf(badRequest, serverError, networkError, nonApiError)
            .map { ConvertModel(it, PromoRegistrationError.Generic(it)) }
    }

    private fun httpError(code: Code, errorBody: String? = null) = ApiResponseError.HttpException(
        code = code,
        message = code.name,
        errorBody = errorBody,
    )
}