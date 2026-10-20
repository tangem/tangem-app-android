package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultRouterTest {

    private val navigation: StackNavigation<Route> = mockk()
    private val navigationProvider: AppNavigationProvider = mockk()

    /** Stack the mocked [navigation] holds; every navigate call applies its transformer to it. */
    private var stack: List<Route> = emptyList()

    private val results = mutableListOf<Boolean>()
    private val onComplete: (Boolean) -> Unit = { results += it }

    private val router = DefaultRouter(navigationProvider, TestingCoroutineDispatcherProvider())

    private val logs = mutableListOf<String>()
    private val logWriter = object : TangemLogger.LogWriter {
        override fun write(
            severity: Severity,
            tag: String,
            message: String,
            throwable: Throwable?,
            shouldSanitize: Boolean,
        ) {
            logs += "$severity $tag $message"
        }
    }

    @BeforeEach
    fun setUp() {
        clearMocks(navigation, navigationProvider)
        results.clear()
        logs.clear()
        TangemLogger.setLogWriters(listOf(logWriter))
        stack = listOf(RouteA)

        every { navigationProvider.getOrCreate() } returns navigation
        every { navigation.navigate(any(), any()) } answers {
            val transformer = firstArg<(List<Route>) -> List<Route>>()
            val callback = secondArg<(List<Route>, List<Route>) -> Unit>()
            val oldStack = stack
            stack = transformer(oldStack)
            callback(stack, oldStack)
        }
    }

    @AfterEach
    fun tearDown() {
        TangemLogger.setLogWriters(emptyList())
    }

    @Test
    fun `GIVEN stack WHEN push THEN route is added and completion is successful`() {
        // Act
        router.push(RouteB, onComplete)

        // Assert
        assertThat(stack).containsExactly(RouteA, RouteB).inOrder()
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN stack WHEN replaceCurrent THEN top route is replaced and completion is successful`() {
        // Arrange
        stack = listOf(RouteA, RouteB)

        // Act
        router.replaceCurrent(RouteC, onComplete)

        // Assert
        assertThat(stack).containsExactly(RouteA, RouteC).inOrder()
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN stack WHEN pop THEN top route is removed and completion is successful`() {
        // Arrange
        stack = listOf(RouteA, RouteB)

        // Act
        router.pop(onComplete)

        // Assert
        assertThat(stack).containsExactly(RouteA)
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN stack WHEN replaceAll THEN stack is replaced and completion is successful`() {
        // Arrange
        stack = listOf(RouteA, RouteB)

        // Act
        router.replaceAll(RouteC, onComplete = onComplete)

        // Assert
        assertThat(stack).containsExactly(RouteC)
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN route class in stack WHEN popTo class THEN routes above it are popped`() {
        // Arrange
        stack = listOf(RouteA, RouteB, RouteC)

        // Act
        router.popTo(RouteA::class, onComplete)

        // Assert
        assertThat(stack).containsExactly(RouteA)
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN navigation throws WHEN push THEN failure is reported as unsuccessful completion`() {
        // Arrange
        every { navigation.navigate(any(), any()) } throws IllegalStateException("Configurations must be unique")

        // Act
        router.push(RouteB, onComplete)

        // Assert
        assertThat(results).containsExactly(false)
    }

    @Test
    fun `GIVEN completion callback throws WHEN push THEN callback is not retried and error is not rethrown`() {
        // Arrange
        val calls = mutableListOf<Boolean>()
        val failingOnComplete: (Boolean) -> Unit = { isSuccess ->
            calls += isSuccess
            throw IllegalStateException("Caller failure")
        }

        // Act
        val actual = runCatching { router.push(RouteB, failingOnComplete) }.exceptionOrNull()

        // Assert
        assertThat(actual).isNull()
        assertThat(calls).containsExactly(true)
        assertThat(stack).containsExactly(RouteA, RouteB).inOrder()
    }

    @Test
    fun `GIVEN navigation and completion callback throw WHEN push THEN failure is reported once and not rethrown`() {
        // Arrange
        every { navigation.navigate(any(), any()) } throws IllegalStateException("Configurations must be unique")
        val calls = mutableListOf<Boolean>()
        val failingOnComplete: (Boolean) -> Unit = { isSuccess ->
            calls += isSuccess
            throw IllegalStateException("Caller failure")
        }

        // Act
        val actual = runCatching { router.push(RouteB, failingOnComplete) }.exceptionOrNull()

        // Assert
        assertThat(actual).isNull()
        assertThat(calls).containsExactly(false)
    }

    @Test
    fun `GIVEN main dispatch needed WHEN push THEN navigation runs only on the main dispatcher`() = runTest {
        // Arrange
        val mainDispatcher = StandardTestDispatcher(testScheduler)
        val router = DefaultRouter(
            navigationProvider = navigationProvider,
            dispatchers = TestingCoroutineDispatcherProvider(main = mainDispatcher, mainImmediate = mainDispatcher),
        )

        // Act
        router.push(RouteB, onComplete)
        val navigatedBeforeDispatch = stack
        runCurrent()

        // Assert
        assertThat(navigatedBeforeDispatch).containsExactly(RouteA)
        assertThat(stack).containsExactly(RouteA, RouteB).inOrder()
        verify(exactly = 1) { navigation.navigate(any(), any()) }
    }

    @Test
    fun `GIVEN stack WHEN navigating THEN every operation is logged as info`() {
        // Arrange
        stack = listOf(RouteA, RouteB)

        // Act
        router.push(RouteC)
        router.replaceCurrent(RouteB)
        router.popTo(RouteA)
        router.replaceAll(RouteA, RouteB)
        router.pop()
        router.popTo(RouteA::class)

        // Assert
        assertThat(logs).containsExactly(
            "Info Router Push RouteC",
            "Info Router Replace a current route with RouteB",
            "Info Router Pop to RouteA",
            "Info Router Replace all routes with [RouteA, RouteB]",
            "Info Router Pop route",
            "Info Router Pop to ${RouteA::class}",
        ).inOrder()
    }

    private data object RouteA : Route
    private data object RouteB : Route
    private data object RouteC : Route
}