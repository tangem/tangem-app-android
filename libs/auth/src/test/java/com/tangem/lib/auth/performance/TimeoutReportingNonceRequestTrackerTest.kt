package com.tangem.lib.auth.performance

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.remote.response.ApiResponse
import com.tangem.lib.auth.api.models.response.NonceApiResponse
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TimeoutReportingNonceRequestTrackerTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val tracker = TimeoutReportingNonceRequestTracker(
        delegate = { _, request -> request() },
        analyticsEventHandler = analyticsEventHandler,
        timeout = TIMEOUT,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no response within timeout WHEN track THEN timeout event sent with type`(model: TypeModel) = runTest {
        // Arrange
        val sentEvents = mutableListOf<AnalyticsEvent>()

        // Act
        tracker.track(model.type) {
            delay(TIMEOUT + 1.seconds)
            nonceSuccess()
        }

        // Assert
        verify { analyticsEventHandler.send(capture(sentEvents)) }
        assertThat(sentEvents.map { it.id to it.params })
            .containsExactly("[Backend Auth] Nonce Request Timeout" to mapOf("Type" to model.expectedTypeValue))
    }

    @Test
    fun `GIVEN response within timeout WHEN track THEN no event sent`() = runTest {
        // Act
        tracker.track(NonceRequestType.DEVICE) {
            delay(TIMEOUT - 1.seconds)
            nonceSuccess()
        }

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN no response within timeout WHEN request completes later THEN its response returned`() = runTest {
        // Arrange
        val expected = nonceSuccess()

        // Act
        val actual = tracker.track(NonceRequestType.AUTHENTICATION) {
            delay(TIMEOUT * 2)
            expected
        }

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN tracker WHEN track THEN delegate receives the request type`() = runTest {
        // Arrange
        val delegatedTypes = mutableListOf<NonceRequestType>()
        val tracker = TimeoutReportingNonceRequestTracker(
            delegate = { type, request ->
                delegatedTypes += type
                request()
            },
            analyticsEventHandler = analyticsEventHandler,
            timeout = TIMEOUT,
        )

        // Act
        tracker.track(NonceRequestType.COLD_WALLET) { nonceSuccess() }

        // Assert
        assertThat(delegatedTypes).containsExactly(NonceRequestType.COLD_WALLET)
    }

    private fun nonceSuccess() = ApiResponse.Success(
        data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
    )

    private fun provideTestModels() = listOf(
        TypeModel(type = NonceRequestType.DEVICE, expectedTypeValue = "Device"),
        TypeModel(type = NonceRequestType.AUTHENTICATION, expectedTypeValue = "Authentication"),
        TypeModel(type = NonceRequestType.COLD_WALLET, expectedTypeValue = "Cold Wallet"),
        TypeModel(type = NonceRequestType.MOBILE_WALLET, expectedTypeValue = "Mobile Wallet"),
    )

    data class TypeModel(val type: NonceRequestType, val expectedTypeValue: String)

    private companion object {
        val TIMEOUT = 5.seconds
    }
}