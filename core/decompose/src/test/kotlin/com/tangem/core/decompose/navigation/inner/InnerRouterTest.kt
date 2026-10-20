package com.tangem.core.decompose.navigation.inner

import com.arkivanov.decompose.router.stack.StackNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InnerRouterTest {

    private val context: AppComponentContext = mockk {
        every { dispatchers } returns TestingCoroutineDispatcherProvider()
    }
    private val stackNavigation: StackNavigation<InnerRoute> = mockk()
    private val fallBackRouter: Router = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(stackNavigation, fallBackRouter)
    }

    @Test
    fun `GIVEN inner stack WHEN contains inner route THEN only routes of that stack are found`() = runTest {
        // Arrange
        every { stackNavigation.navigate(any(), any()) } answers {
            val transformer = firstArg<(List<InnerRoute>) -> List<InnerRoute>>()
            val callback = secondArg<(List<InnerRoute>, List<InnerRoute>) -> Unit>()
            val stack = listOf<InnerRoute>(InnerRoute.First)
            callback(transformer(stack), stack)
        }
        val router = context.InnerRouter(stackNavigation = stackNavigation, fallBackRouter = fallBackRouter)

        // Act
        val actual = listOf(InnerRoute.First, InnerRoute.Second).map { router.contains(it) }

        // Assert
        assertThat(actual).containsExactly(true, false).inOrder()
    }

    @Test
    fun `GIVEN outer route WHEN contains THEN fallback router answers`() = runTest {
        // Arrange
        coEvery { fallBackRouter.contains(OuterRoute) } returns true
        val router = context.InnerRouter(stackNavigation = stackNavigation, fallBackRouter = fallBackRouter)

        // Act
        val actual = router.contains(OuterRoute)

        // Assert
        assertThat(actual).isTrue()
        verify(exactly = 0) { stackNavigation.navigate(any(), any()) }
    }

    @Test
    fun `GIVEN outer route class WHEN popToMatching THEN fallback router pops`() {
        // Arrange
        val predicate: (OuterRoute) -> Boolean = { true }
        every { fallBackRouter.popToMatching(OuterRoute::class, predicate, any()) } returns Unit
        val router = context.InnerRouter(stackNavigation = stackNavigation, fallBackRouter = fallBackRouter)

        // Act
        router.popToMatching(OuterRoute::class, predicate)

        // Assert
        verify(exactly = 1) { fallBackRouter.popToMatching(OuterRoute::class, predicate, any()) }
    }

    private sealed interface InnerRoute : Route {
        data object First : InnerRoute
        data object Second : InnerRoute
    }

    private data object OuterRoute : Route
}