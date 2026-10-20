package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RouterExtTest {

    @ParameterizedTest
    @ProvideTestModels
    fun popToOrReplaceCurrent(model: PopToOrReplaceCurrentModel) {
        // Arrange
        var stack = model.initialStack
        val navigation: StackNavigation<Route> = mockk()
        every { navigation.navigate(any(), any()) } answers {
            val transformer = firstArg<(List<Route>) -> List<Route>>()
            val callback = secondArg<(List<Route>, List<Route>) -> Unit>()
            val oldStack = stack
            stack = transformer(oldStack)
            callback(stack, oldStack)
        }
        val navigationProvider: AppNavigationProvider = mockk {
            every { getOrCreate() } returns navigation
        }
        val router = DefaultRouter(navigationProvider, TestingCoroutineDispatcherProvider())

        // Act
        router.popToOrReplaceCurrent(TokenDetails)

        // Assert
        assertThat(stack).containsExactlyElementsIn(model.expectedStack).inOrder()
    }

    @ParameterizedTest
    @MethodSource("provideMatchingModels")
    fun popToOrReplaceCurrentMatching(model: PopToOrReplaceCurrentModel) {
        // Arrange
        var stack = model.initialStack
        val navigation: StackNavigation<Route> = mockk()
        every { navigation.navigate(any(), any()) } answers {
            val transformer = firstArg<(List<Route>) -> List<Route>>()
            val callback = secondArg<(List<Route>, List<Route>) -> Unit>()
            val oldStack = stack
            stack = transformer(oldStack)
            callback(stack, oldStack)
        }
        val navigationProvider: AppNavigationProvider = mockk {
            every { getOrCreate() } returns navigation
        }
        val router = DefaultRouter(navigationProvider, TestingCoroutineDispatcherProvider())

        // Act
        router.popToOrReplaceCurrent(Details(id = 1)) { it.id == 1 }

        // Assert
        assertThat(stack).containsExactlyElementsIn(model.expectedStack).inOrder()
    }

    private fun provideMatchingModels() = listOf(
        PopToOrReplaceCurrentModel(
            initialStack = listOf(Wallet, Details(id = 1, flag = true)),
            expectedStack = listOf(Wallet, Details(id = 1, flag = true)),
        ),
        PopToOrReplaceCurrentModel(
            initialStack = listOf(Wallet, Details(id = 1, flag = true), Send),
            expectedStack = listOf(Wallet, Details(id = 1, flag = true)),
        ),
        PopToOrReplaceCurrentModel(
            initialStack = listOf(Wallet, Details(id = 2), Send),
            expectedStack = listOf(Wallet, Details(id = 2), Details(id = 1)),
        ),
    )

    private fun provideTestModels() = listOf(
        PopToOrReplaceCurrentModel(
            initialStack = listOf(Wallet, Send),
            expectedStack = listOf(Wallet, TokenDetails),
        ),
        PopToOrReplaceCurrentModel(
            initialStack = listOf(Wallet, TokenDetails, Send),
            expectedStack = listOf(Wallet, TokenDetails),
        ),
        PopToOrReplaceCurrentModel(
            initialStack = listOf(Wallet, TokenDetails, OtherTokenDetails, Send),
            expectedStack = listOf(Wallet, TokenDetails),
        ),
        PopToOrReplaceCurrentModel(
            initialStack = listOf(Wallet, TokenDetails),
            expectedStack = listOf(Wallet, TokenDetails),
        ),
    )

    data class PopToOrReplaceCurrentModel(val initialStack: List<Route>, val expectedStack: List<Route>)

    private data object Wallet : Route
    private data object Send : Route
    private data object TokenDetails : Route
    private data object OtherTokenDetails : Route
    private data class Details(val id: Int, val flag: Boolean = false) : Route
}