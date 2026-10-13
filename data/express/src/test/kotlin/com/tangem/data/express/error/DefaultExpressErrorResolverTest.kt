package com.tangem.data.express.error

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.express.converter.ExpressErrorConverter
import com.tangem.domain.express.models.ExpressError
import com.tangem.grow.datasource.express.models.response.ExpressErrorResponse
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import org.junit.jupiter.api.Test

internal class DefaultExpressErrorResolverTest {

    private val jsonAdapter = legacyDataStoreMoshi.adapter(ExpressErrorResponse::class.java)
    private val resolver = DefaultExpressErrorResolver(ExpressErrorConverter(jsonAdapter))

    @Test
    fun `GIVEN http exception with intent of another wallet code WHEN resolve THEN that typed error is returned`() {
        // Arrange
        val exception = httpException(errorBody = """{"error":{"code":2610,"description":"Wrong wallet"}}""")

        // Act
        val actual = resolver.resolve(exception)

        // Assert
        assertThat(actual).isEqualTo(ExpressError.IntentOfAnotherWalletError(code = 2610))
    }

    @Test
    fun `GIVEN http exception with unknown intent code WHEN resolve THEN data error with that code is returned`() {
        // Arrange
        val exception = httpException(errorBody = """{"error":{"code":2600,"description":"Not found"}}""")

        // Act
        val actual = resolver.resolve(exception)

        // Assert
        assertThat(actual).isEqualTo(ExpressError.DataError(code = 2600, description = "Not found"))
    }

    @Test
    fun `GIVEN http exception with malformed body WHEN resolve THEN unknown error is returned`() {
        // Arrange
        val exception = httpException(errorBody = "not json")

        // Act
        val actual = resolver.resolve(exception)

        // Assert
        assertThat(actual).isEqualTo(ExpressError.UnknownError)
    }

    @Test
    fun `GIVEN http exception with missing body WHEN resolve THEN unknown error is returned`() {
        // Arrange
        val exception = httpException(errorBody = null)

        // Act
        val actual = resolver.resolve(exception)

        // Assert
        assertThat(actual).isEqualTo(ExpressError.UnknownError)
    }

    @Test
    fun `GIVEN non-http failure WHEN resolve THEN unknown error is returned`() {
        // Arrange
        val exception = ApiResponseError.NetworkException()

        // Act
        val actual = resolver.resolve(exception)

        // Assert
        assertThat(actual).isEqualTo(ExpressError.UnknownError)
    }

    private fun httpException(errorBody: String?) = ApiResponseError.HttpException(
        code = ApiResponseError.HttpException.Code.FORBIDDEN,
        message = null,
        errorBody = errorBody,
    )
}