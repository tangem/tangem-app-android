package com.tangem.tap.routing

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.wallet.R
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultNavigationFailureHandlerTest {

    private val snackbarHandler: SnackbarHandler = mockk(relaxed = true)
    private val analyticsExceptionHandler: AnalyticsExceptionHandler = mockk(relaxed = true)
    private val config: AppRouterConfig = mockk(relaxed = true)

    private val handler = DefaultNavigationFailureHandler(config, analyticsExceptionHandler)

    @BeforeEach
    fun resetMocks() {
        clearMocks(snackbarHandler, analyticsExceptionHandler, config)
        every { config.snackbarHandler } returns snackbarHandler
    }

    @Test
    fun `GIVEN snackbar handler WHEN navigation fails THEN failure is sent to analytics and shown to the user`() {
        // Act
        handler.onNavigationFailed("Unable to push Wallet")

        // Assert
        verify(exactly = 1) { analyticsExceptionHandler.sendException(any<ExceptionAnalyticsEvent>()) }
        verify(exactly = 1) {
            snackbarHandler.showSnackbar(
                text = R.string.common_unknown_error,
                buttonTitle = R.string.common_ok,
                action = any(),
            )
        }
    }

    @Test
    fun `GIVEN no snackbar handler WHEN navigation fails THEN failure is only sent to analytics`() {
        // Arrange
        every { config.snackbarHandler } returns null

        // Act
        handler.onNavigationFailed("Unable to push Wallet")

        // Assert
        verify(exactly = 1) { analyticsExceptionHandler.sendException(any<ExceptionAnalyticsEvent>()) }
        verify(exactly = 0) { snackbarHandler.showSnackbar(text = any<Int>(), buttonTitle = any(), action = any()) }
    }
}