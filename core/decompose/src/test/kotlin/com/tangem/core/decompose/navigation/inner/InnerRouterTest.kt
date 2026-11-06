package com.tangem.core.decompose.navigation.inner

import com.arkivanov.decompose.router.stack.StackNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

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
    fun `GIVEN transformer of inner routes WHEN navigate THEN inner stack is rebuilt`() {
        // Arrange
        var stack = listOf<InnerRoute>(InnerRoute.First, InnerRoute.Second)
        every { stackNavigation.navigate(any(), any()) } answers {
            val transformer = firstArg<(List<InnerRoute>) -> List<InnerRoute>>()
            val callback = secondArg<(List<InnerRoute>, List<InnerRoute>) -> Unit>()
            val oldStack = stack
            stack = transformer(oldStack)
            callback(stack, oldStack)
        }
        val results = mutableListOf<Boolean>()
        val router = context.InnerRouter(stackNavigation = stackNavigation, fallBackRouter = fallBackRouter)

        // Act
        router.navigate(transformer = { it.filterNot { route -> route == InnerRoute.First } }) { results += it }

        // Assert
        assertThat(stack).containsExactly(InnerRoute.Second)
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN transformer adding an outer route WHEN navigate THEN inner stack is unchanged and completion fails`() {
        // Arrange
        var stack = listOf<InnerRoute>(InnerRoute.First)
        every { stackNavigation.navigate(any(), any()) } answers {
            val transformer = firstArg<(List<InnerRoute>) -> List<InnerRoute>>()
            val callback = secondArg<(List<InnerRoute>, List<InnerRoute>) -> Unit>()
            val oldStack = stack
            stack = transformer(oldStack)
            callback(stack, oldStack)
        }
        val results = mutableListOf<Boolean>()
        val router = context.InnerRouter(stackNavigation = stackNavigation, fallBackRouter = fallBackRouter)

        // Act
        router.navigate(transformer = { it + OuterRoute }) { results += it }

        // Assert
        assertThat(stack).containsExactly(InnerRoute.First)
        assertThat(results).containsExactly(false)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PopTo {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN inner stack WHEN popTo inner route THEN completion tells whether the route ended up on top`(
            model: PopToModel,
        ) {
            // Arrange
            val results = mutableListOf<Boolean>()
            val router = createRouterOver(model.stack)

            // Act
            router.popTo(InnerRoute.First) { results += it }

            // Assert
            assertThat(results).containsExactly(model.expectedIsSuccess)
        }

        @Test
        fun `GIVEN route of the inner class on top WHEN popTo inner route class THEN completion is successful`() {
            // Arrange
            val classStackNavigation: StackNavigation<ClassRoute> = mockk()
            val stack = listOf(ClassRoute(id = 1), ClassRoute(id = 2))
            every { classStackNavigation.navigate(any(), any()) } answers {
                val transformer = firstArg<(List<ClassRoute>) -> List<ClassRoute>>()
                val callback = secondArg<(List<ClassRoute>, List<ClassRoute>) -> Unit>()
                callback(transformer(stack), stack)
            }
            val results = mutableListOf<Boolean>()
            val router = context.InnerRouter(stackNavigation = classStackNavigation, fallBackRouter = fallBackRouter)

            // Act
            router.popTo(ClassRoute::class) { results += it }

            // Assert
            assertThat(results).containsExactly(true)
        }

        private fun createRouterOver(initialStack: List<InnerRoute>): Router {
            var stack = initialStack
            every { stackNavigation.navigate(any(), any()) } answers {
                val transformer = firstArg<(List<InnerRoute>) -> List<InnerRoute>>()
                val callback = secondArg<(List<InnerRoute>, List<InnerRoute>) -> Unit>()
                val oldStack = stack
                stack = transformer(oldStack)
                callback(stack, oldStack)
            }
            return context.InnerRouter(stackNavigation = stackNavigation, fallBackRouter = fallBackRouter)
        }

        private fun provideTestModels() = listOf(
            PopToModel(stack = listOf(InnerRoute.First, InnerRoute.Second), expectedIsSuccess = true),
            PopToModel(stack = listOf(InnerRoute.First), expectedIsSuccess = true),
            PopToModel(stack = listOf(InnerRoute.Second), expectedIsSuccess = false),
        )
    }

    data class PopToModel(val stack: List<InnerRoute>, val expectedIsSuccess: Boolean)

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

    @Test
    fun `GIVEN failed operation WHEN default completion handler is called THEN fallback router handles it`() {
        // Arrange
        every { fallBackRouter.defaultCompletionHandler(any(), any()) } returns Unit
        val router = context.InnerRouter(stackNavigation = stackNavigation, fallBackRouter = fallBackRouter)

        // Act
        router.defaultCompletionHandler(isSuccess = false, errorMessage = "Unable to pop route")

        // Assert
        verify(exactly = 1) { fallBackRouter.defaultCompletionHandler(false, "Unable to pop route") }
    }

    sealed interface InnerRoute : Route {
        data object First : InnerRoute
        data object Second : InnerRoute
    }

    private data object OuterRoute : Route

    private data class ClassRoute(val id: Int) : Route
}