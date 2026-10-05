package com.tangem.features.jointaccount.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.JointAccountError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a failure cause WHEN error event built THEN params match`(model: ErrorModel) {
        // Act
        val event = JointAccountError(source = ErrorSource.Sign, cause = model.cause)

        // Assert
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    data class ErrorModel(val cause: Throwable, val expectedParams: Map<String, String>)

    private fun provideTestModels() = listOf(
        ErrorModel(
            cause = httpError(errorBody = """{"code":"NONCE_MISMATCH"}"""),
            expectedParams = params(errorCode = "NONCE_MISMATCH"),
        ),
        ErrorModel(
            cause = httpError(errorBody = """{"message":"no code here"}"""),
            expectedParams = params(errorCode = "400"),
        ),
        ErrorModel(
            cause = httpError(errorBody = "not a json"),
            expectedParams = params(errorCode = "400"),
        ),
        ErrorModel(
            cause = httpError(errorBody = null),
            expectedParams = params(errorCode = "400"),
        ),
        ErrorModel(
            cause = IllegalStateException(HTTP_MESSAGE),
            expectedParams = params(errorCode = null),
        ),
    )

    private fun httpError(errorBody: String?) = ApiResponseError.HttpException(
        code = ApiResponseError.HttpException.Code.BAD_REQUEST,
        message = HTTP_MESSAGE,
        errorBody = errorBody,
    )

    private fun params(errorCode: String?): Map<String, String> = buildMap {
        put(AnalyticsParam.SOURCE, "Sign")
        errorCode?.let { put(AnalyticsParam.ERROR_CODE, it) }
        put(AnalyticsParam.ERROR_MESSAGE, HTTP_MESSAGE)
    }

    private companion object {
        const val HTTP_MESSAGE = "Bad request"
    }
}