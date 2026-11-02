package com.tangem.data.onramp

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.onramp.converters.error.OnrampErrorConverter
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.model.error.OnrampPairsError
import com.tangem.domain.onramp.model.error.OnrampRedirectError
import com.tangem.grow.datasource.express.models.response.ExpressErrorResponse
import org.junit.jupiter.api.Test

internal class DefaultOnrampErrorResolverTest {

    private val resolver = DefaultOnrampErrorResolver(
        OnrampErrorConverter(
            Moshi.Builder().add(BigDecimalAdapter()).build().adapter(ExpressErrorResponse::class.java),
        ),
    )

    private class BigDecimalAdapter {
        @FromJson fun fromJson(value: String) = java.math.BigDecimal(value)
        @ToJson fun toJson(value: java.math.BigDecimal) = value.toString()
    }

    @Test
    fun `GIVEN http error with express body WHEN resolve THEN converter result is returned`() {
        // Act
        val actual = resolver.resolve(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.BAD_REQUEST,
                message = "bad request",
                errorBody = """{"error":{"code":4001,"description":"declined","value":null}}""",
            ),
        )

        // Assert
        assertThat(actual).isEqualTo(OnrampError.DataError("4001", "declined"))
    }

    @Test
    fun `GIVEN redirect verification failure WHEN resolve THEN redirect error is mapped`() {
        // Act
        val actual = resolver.resolve(OnrampRedirectError.VerificationFailed())

        // Assert
        assertThat(actual).isEqualTo(OnrampError.RedirectError.VerificationFailed)
    }

    @Test
    fun `GIVEN wrong request id WHEN resolve THEN redirect error is mapped`() {
        // Act
        val actual = resolver.resolve(OnrampRedirectError.WrongRequestId())

        // Assert
        assertThat(actual).isEqualTo(OnrampError.RedirectError.WrongRequestId)
    }

    @Test
    fun `GIVEN missing pairs WHEN resolve THEN pairs error is mapped`() {
        // Act
        val actual = resolver.resolve(OnrampPairsError.PairsNotFound())

        // Assert
        assertThat(actual).isEqualTo(OnrampError.PairsNotFound)
    }

    @Test
    fun `GIVEN unknown throwable WHEN resolve THEN domain error preserves message`() {
        // Act
        val actual = resolver.resolve(IllegalStateException("broken"))

        // Assert
        assertThat(actual).isEqualTo(OnrampError.DomainError("broken"))
    }
}