package com.tangem.lib.auth.performance

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.PerformanceTrace
import com.tangem.core.analytics.api.PerformanceTracer
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.lib.auth.api.models.response.NonceApiResponse
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TracingNonceRequestTrackerTest {

    private val trace: PerformanceTrace = mockk(relaxUnitFun = true)
    private val performanceTracer: PerformanceTracer = mockk()

    private val tracker = TracingNonceRequestTracker(performanceTracer = performanceTracer)

    @BeforeEach
    fun resetMocks() {
        clearMocks(trace, performanceTracer)
        every { performanceTracer.start(any()) } returns trace
    }

    @Test
    fun `GIVEN successful request WHEN track THEN trace marks success and stops`() = runTest {
        // Arrange
        val response = ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )

        // Act
        val actual = tracker.track(type = NonceRequestType.DEVICE) { response }

        // Assert
        assertThat(actual).isEqualTo(response)
        verify(exactly = 1) { performanceTracer.start(NonceRequestType.DEVICE.traceName) }
        verifyOrder {
            trace.putAttribute("result", "success")
            trace.stop()
        }
    }

    @Test
    fun `GIVEN failed request WHEN track THEN trace marks error and stops`() = runTest {
        // Arrange
        @Suppress("UNCHECKED_CAST")
        val response = ApiResponse.Error(
            cause = ApiResponseError.UnknownException(IllegalStateException()),
        ) as ApiResponse<NonceApiResponse>

        // Act
        tracker.track(type = NonceRequestType.AUTHENTICATION) { response }

        // Assert
        verifyOrder {
            trace.putAttribute("result", "error")
            trace.stop()
        }
    }

    @Test
    fun `GIVEN request throws WHEN track THEN trace stops and exception propagates`() = runTest {
        // Arrange
        val exception = IllegalStateException("network")

        // Act
        val actual = runCatching { tracker.track(type = NonceRequestType.COLD_WALLET) { throw exception } }

        // Assert
        assertThat(actual.exceptionOrNull()).isEqualTo(exception)
        verify(exactly = 1) { trace.stop() }
        verify(inverse = true) { trace.putAttribute(any(), any()) }
    }

    @Test
    fun `GIVEN tracer fails to start WHEN track THEN request result is returned`() = runTest {
        // Arrange
        every { performanceTracer.start(any()) } throws IllegalStateException("not initialized")
        val response = ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )

        // Act
        val actual = tracker.track(type = NonceRequestType.MOBILE_WALLET) { response }

        // Assert
        assertThat(actual).isEqualTo(response)
    }
}