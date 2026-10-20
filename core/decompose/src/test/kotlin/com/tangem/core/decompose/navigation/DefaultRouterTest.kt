package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

    @BeforeEach
    fun setUp() {
        clearMocks(navigation, navigationProvider)
        results.clear()
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
    fun `GIVEN stack WHEN contains THEN only routes of that stack are found and stack is unchanged`() = runTest {
        // Arrange
        stack = listOf(RouteA, RouteB)

        // Act
        val actual = listOf(RouteA, RouteB, RouteC).map { router.contains(it) }

        // Assert
        assertThat(actual).containsExactly(true, true, false).inOrder()
        assertThat(stack).containsExactly(RouteA, RouteB).inOrder()
    }

    @Test
    fun `GIVEN routes of the class WHEN popToMatching THEN popped to the last route satisfying the predicate`() {
        // Arrange
        stack = listOf(RouteA, Details(id = 1, flag = true), Details(id = 2), Details(id = 1, flag = false), RouteB)

        // Act
        router.popToMatching(Details::class, predicate = { it.id == 1 }, onComplete = onComplete)

        // Assert
        assertThat(stack)
            .containsExactly(RouteA, Details(id = 1, flag = true), Details(id = 2), Details(id = 1, flag = false))
            .inOrder()
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN routes of a class deeper in stack WHEN navigate removes them THEN stack is rebuilt and completion is successful`() {
        // Arrange
        stack = listOf(RouteA, Details(id = 1), RouteB, Details(id = 2))

        // Act
        router.navigate(transformer = { it.filterNot { route -> route is Details } + RouteC }, onComplete = onComplete)

        // Assert
        assertThat(stack).containsExactly(RouteA, RouteB, RouteC).inOrder()
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN matching route on top WHEN popToMatching THEN stack is unchanged and completion is successful`() {
        // Arrange
        stack = listOf(RouteA, Details(id = 1, flag = true))

        // Act
        router.popToMatching(Details::class, predicate = { it.id == 1 }, onComplete = onComplete)

        // Assert
        assertThat(stack).containsExactly(RouteA, Details(id = 1, flag = true)).inOrder()
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN no route satisfying the predicate WHEN popToMatching THEN stack is unchanged and completion fails`() {
        // Arrange
        stack = listOf(RouteA, Details(id = 2), RouteB)

        // Act
        router.popToMatching(Details::class, predicate = { it.id == 1 }, onComplete = onComplete)

        // Assert
        assertThat(stack).containsExactly(RouteA, Details(id = 2), RouteB).inOrder()
        assertThat(results).containsExactly(false)
    }

    private data object RouteA : Route
    private data object RouteB : Route
    private data object RouteC : Route
    private data class Details(val id: Int, val flag: Boolean = false) : Route
}