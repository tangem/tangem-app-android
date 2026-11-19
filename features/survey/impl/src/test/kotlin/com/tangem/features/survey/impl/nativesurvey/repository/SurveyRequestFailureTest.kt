package com.tangem.features.survey.impl.nativesurvey.repository

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.tangem.test.core.ProvideTestModels
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import retrofit2.HttpException
import retrofit2.Response
import java.io.EOFException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SurveyRequestFailureTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a request error WHEN mapped THEN returns the survey failure`(model: FailureModel) {
        // Act
        val actual = model.error.toSurveyRequestFailure()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FailureModel(error = httpError(code = 404), expected = SurveyRequestFailure.Http(code = 404)),
        FailureModel(error = httpError(code = 429), expected = SurveyRequestFailure.Http(code = 429)),
        FailureModel(error = httpError(code = 503), expected = SurveyRequestFailure.Http(code = 503)),
        FailureModel(error = SocketTimeoutException(), expected = SurveyRequestFailure.Network),
        FailureModel(error = UnknownHostException(), expected = SurveyRequestFailure.Network),
        FailureModel(error = JsonDataException(), expected = SurveyRequestFailure.Parse),
        FailureModel(error = JsonEncodingException("malformed"), expected = SurveyRequestFailure.Parse),
        FailureModel(error = EOFException(), expected = SurveyRequestFailure.Parse),
        FailureModel(error = KotlinNullPointerException(), expected = SurveyRequestFailure.Parse),
    )

    private fun httpError(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody(null)))

    data class FailureModel(val error: Throwable, val expected: SurveyRequestFailure)
}