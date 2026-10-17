package com.tangem.core.decompose.navigation.inner

import com.arkivanov.decompose.router.stack.StackNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InnerRouterTest {

    private val context: AppComponentContext = mockk()
    private val stackNavigation: StackNavigation<InnerRoute> = mockk()
    private val fallBackRouter: Router = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(fallBackRouter)
    }

    @Test
    fun `GIVEN current stack WHEN contains inner route THEN only routes of that stack are found`() {
        // Arrange
        val router = context.InnerRouter(
            stackNavigation = stackNavigation,
            fallBackRouter = fallBackRouter,
            currentStack = { listOf(InnerRoute.First) },
        )

        // Act
        val actual = listOf(InnerRoute.First, InnerRoute.Second).map(router::contains)

        // Assert
        assertThat(actual).containsExactly(true, false).inOrder()
    }

    @Test
    fun `GIVEN no current stack WHEN contains inner route THEN not found`() {
        // Arrange
        val router = context.InnerRouter(stackNavigation = stackNavigation, fallBackRouter = fallBackRouter)

        // Act
        val actual = router.contains(InnerRoute.First)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN outer route WHEN contains THEN fallback router answers`() {
        // Arrange
        every { fallBackRouter.contains(OuterRoute) } returns true
        val router = context.InnerRouter(
            stackNavigation = stackNavigation,
            fallBackRouter = fallBackRouter,
            currentStack = { emptyList() },
        )

        // Act
        val actual = router.contains(OuterRoute)

        // Assert
        assertThat(actual).isTrue()
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