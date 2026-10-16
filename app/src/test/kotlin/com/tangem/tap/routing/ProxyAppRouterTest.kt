package com.tangem.tap.routing

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.tap.common.SnackbarHandler
import com.tangem.tap.routing.configurator.AppRouterConfig
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProxyAppRouterTest {

    private val innerRouter = mockk<Router>(relaxed = true)
    private val snackbarHandler = mockk<SnackbarHandler>(relaxed = true)
    private val analyticsExceptionHandler = mockk<AnalyticsExceptionHandler>(relaxed = true)

    private val config = mockk<AppRouterConfig>(relaxed = true) {
        every { componentRouter } returns innerRouter
        every { stack } returns listOf(AppRoute.Wallet)
        every { snackbarHandler } returns this@ProxyAppRouterTest.snackbarHandler
        every { initializedState } returns MutableStateFlow(true)
    }

    @AfterEach
    fun tearDown() {
        clearMocks(innerRouter, snackbarHandler, analyticsExceptionHandler, config)
        every { config.componentRouter } returns innerRouter
        every { config.stack } returns listOf(AppRoute.Wallet)
        every { config.snackbarHandler } returns snackbarHandler
        every { config.initializedState } returns MutableStateFlow(true)
    }

    private fun createRouter(): ProxyAppRouter = ProxyAppRouter(config, analyticsExceptionHandler)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Push {

        @Test
        fun `delegates to inner router`() {
            // Arrange
            val router = createRouter()
            val route = AppRoute.AppSettings

            // Act
            router.push(route)

            // Assert
            verify { innerRouter.push(route, any()) }
        }

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReplaceCurrent {

        @Test
        fun `delegates to inner router`() {
            // Arrange
            val router = createRouter()
            val route = AppRoute.AppSettings

            // Act
            router.replaceCurrent(route)

            // Assert
            verify { innerRouter.replaceCurrent(route, any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReplaceAll {

        @Test
        fun `delegates to inner router`() {
            // Arrange
            val router = createRouter()
            val route = AppRoute.Wallet

            // Act
            router.replaceAll(route)

            // Assert
            verify { innerRouter.replaceAll(route, onComplete = any()) }
        }

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Pop {

        @Test
        fun `delegates to inner router`() {
            // Arrange
            val router = createRouter()

            // Act
            router.pop()

            // Assert
            verify { innerRouter.pop(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PopTo {

        @Test
        fun `delegates to inner router with route`() {
            // Arrange
            val router = createRouter()
            val route = AppRoute.Wallet

            // Act
            router.popTo(route)

            // Assert
            verify { innerRouter.popTo(route, any()) }
        }

        @Test
        fun `delegates to inner router with routeClass`() {
            // Arrange
            val router = createRouter()

            // Act
            router.popTo(AppRoute.Wallet::class)

            // Assert
            verify { innerRouter.popTo(AppRoute.Wallet::class, any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DefaultCompletionHandler {

        @Test
        fun `does nothing on success`() {
            // Arrange
            val router = createRouter()

            // Act
            router.defaultCompletionHandler(isSuccess = true, errorMessage = "error")

            // Assert
            verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
            verify(exactly = 0) { snackbarHandler.showSnackbar(text = any<Int>(), buttonTitle = any(), action = any()) }
        }

        @Test
        fun `sends analytics and shows snackbar on failure`() {
            // Arrange
            val router = createRouter()

            // Act
            router.defaultCompletionHandler(isSuccess = false, errorMessage = "Navigation failed")

            // Assert
            verify { analyticsExceptionHandler.sendException(any<ExceptionAnalyticsEvent>()) }
            verify { snackbarHandler.showSnackbar(text = any<Int>(), buttonTitle = any(), action = any()) }
        }

        @Test
        fun `sends analytics without snackbar when handler is null`() {
            // Arrange
            every { config.snackbarHandler } returns null
            val router = createRouter()

            // Act
            router.defaultCompletionHandler(isSuccess = false, errorMessage = "Navigation failed")

            // Assert
            verify { analyticsExceptionHandler.sendException(any<ExceptionAnalyticsEvent>()) }
            verify(exactly = 0) { snackbarHandler.showSnackbar(text = any<Int>(), buttonTitle = any(), action = any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Stack {

        @Test
        fun `returns config stack`() {
            // Arrange
            val router = createRouter()
            val expected = listOf(AppRoute.Wallet)

            // Act
            val actual = router.stack

            // Assert
            assertThat(actual).isEqualTo(expected)
        }
    }
}