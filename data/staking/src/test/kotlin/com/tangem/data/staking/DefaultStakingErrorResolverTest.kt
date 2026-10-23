package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.staking.converters.error.StakeKitErrorConverter
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.grow.datasource.stakekit.models.response.model.error.StakeKitErrorResponse
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStakingErrorResolverTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val resolver = DefaultStakingErrorResolver(
        analyticsEventHandler = analyticsEventHandler,
        stakeKitErrorConverter = StakeKitErrorConverter(
            Moshi.Builder().build().adapter(StakeKitErrorResponse::class.java),
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    private fun http(body: String?) = ApiResponseError.HttpException(
        code = ApiResponseError.HttpException.Code.BAD_REQUEST,
        message = "bad",
        errorBody = body,
    )

    @Test
    fun `GIVEN http error with valid json WHEN resolve THEN stakeKit api error and analytics sent`() {
        // Arrange
        val body = """{"message":"Insufficient balance","code":1001,"path":"/v1/actions/enter",""" +
            """"details":{"amount":"1.5"}}"""

        // Act
        val actual = resolver.resolve(http(body))

        // Assert
        val expected = StakingError.StakeKitApiError(
            message = "Insufficient balance",
            code = 1001,
            details = StakingError.StakeKitApiError.ErrorDetails(BigDecimal("1.5")),
            methodName = "/v1/actions/enter",
        )
        assertThat(actual).isEqualTo(expected)
        verify(exactly = 1) { analyticsEventHandler.send(StakingAnalyticsEvent.StakeKitApiError(expected)) }
    }

    @Test
    fun `GIVEN details amount not a number WHEN resolve THEN details amount null`() {
        // Act
        val actual = resolver.resolve(http("""{"message":"m","code":1,"details":{"amount":"abc"}}"""))

        // Assert
        val expected = StakingError.StakeKitApiError(
            message = "m",
            code = 1,
            details = StakingError.StakeKitApiError.ErrorDetails(null),
            methodName = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN geo blocked 403 body WHEN resolve THEN stakeKit api error without details`() {
        // Arrange
        val body = """{"type":"GEO_LOCATION","countryCode":"US","regionCode":"CA","message":"Forbidden","code":403}"""

        // Act
        val actual = resolver.resolve(http(body))

        // Assert
        val expected = StakingError.StakeKitApiError(
            message = "Forbidden",
            code = 403,
            details = null,
            methodName = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN http error with malformed body WHEN resolve THEN stakeKit unknown error with raw body`() {
        // Act
        val actual = resolver.resolve(http("not json"))

        // Assert
        val expected = StakingError.StakeKitUnknownError("not json")
        assertThat(actual).isEqualTo(expected)
        verify { analyticsEventHandler.send(StakingAnalyticsEvent.StakeKitApiUnknownError(expected)) }
    }

    @Test
    fun `GIVEN http error without body WHEN resolve THEN stakeKit unknown error with empty string`() {
        // Act
        val actual = resolver.resolve(http(null))

        // Assert
        val expected = StakingError.StakeKitUnknownError("")
        assertThat(actual).isEqualTo(expected)
        verify { analyticsEventHandler.send(StakingAnalyticsEvent.StakeKitApiUnknownError(expected)) }
    }

    @Test
    fun `GIVEN http error with json null literal WHEN resolve THEN stakeKit unknown error`() {
        // Act
        val actual = resolver.resolve(http("null"))

        // Assert
        assertThat(actual).isEqualTo(StakingError.StakeKitUnknownError("null"))
    }

    @Test
    fun `GIVEN non http throwable WHEN resolve THEN domain error with message and analytics sent`() {
        // Act
        val actual = resolver.resolve(IllegalStateException("boom"))

        // Assert
        val expected = StakingError.DomainError("boom")
        assertThat(actual).isEqualTo(expected)
        verify { analyticsEventHandler.send(StakingAnalyticsEvent.DomainError(expected)) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN any throwable WHEN resolve THEN exactly one analytics event sent`(model: ThrowableModel) {
        // Act
        resolver.resolve(model.throwable)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    private fun provideTestModels() = listOf(
        ThrowableModel("http with empty json", http("{}")),
        ThrowableModel("http with malformed body", http("x")),
        ThrowableModel("runtime exception", RuntimeException("r")),
    )

    internal data class ThrowableModel(val name: String, val throwable: Throwable) {
        override fun toString(): String = name
    }
}