package com.tangem.common.routing.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class AppRouterExtTest {

    private val userWalletId = UserWalletId(stringValue = "0011")
    private val tether = mockk<CryptoCurrency>(relaxed = true)
    private val tron = mockk<CryptoCurrency>(relaxed = true)

    private val tetherDetails = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = tether)
    private val tronDetails = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = tron)
    private val send = AppRoute.Send(userWalletId = userWalletId, currency = tether)
    private val wallet = AppRoute.Wallet

    @Test
    fun `GIVEN route is not in stack WHEN popAndPush THEN current route replaced with it`() {
        // Arrange
        val router = FakeAppRouter(wallet, send)

        // Act
        router.popAndPush(tetherDetails)

        // Assert
        assertThat(router.stack).containsExactly(wallet, tetherDetails).inOrder()
        assertThat(router.errors).isEmpty()
    }

    @Test
    fun `GIVEN route is under the current one WHEN popAndPush THEN popped to it without error`() {
        // Arrange
        // Gasless send: the fee currency is the token the send flow was opened from
        val router = FakeAppRouter(wallet, tetherDetails, send)

        // Act
        router.popAndPush(tetherDetails)

        // Assert
        assertThat(router.stack).containsExactly(wallet, tetherDetails).inOrder()
        assertThat(router.errors).isEmpty()
    }

    @Test
    fun `GIVEN route is deep in stack WHEN popAndPush THEN popped to it without error`() {
        // Arrange
        val router = FakeAppRouter(wallet, tetherDetails, tronDetails, send)

        // Act
        router.popAndPush(tetherDetails)

        // Assert
        assertThat(router.stack).containsExactly(wallet, tetherDetails).inOrder()
        assertThat(router.errors).isEmpty()
    }

    @Test
    fun `GIVEN route is the current one WHEN popAndPush THEN stack is unchanged`() {
        // Arrange
        val router = FakeAppRouter(wallet, tetherDetails)

        // Act
        router.popAndPush(tetherDetails)

        // Assert
        assertThat(router.stack).containsExactly(wallet, tetherDetails).inOrder()
        assertThat(router.errors).isEmpty()
    }

    /**
     * Mirrors the Decompose-backed router: pushing a route that is already in the stack fails –
     * silently when it is on top, with an exception when it is deeper.
     */
    private class FakeAppRouter(vararg initialStack: AppRoute) : AppRouter {

        override var stack: List<AppRoute> = initialStack.toList()
            private set

        val errors = mutableListOf<String>()

        override fun push(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
            when {
                stack.lastOrNull() == route -> onComplete(false)
                stack.contains(route) -> error("Configurations must be unique: $route")
                else -> {
                    stack = stack + route
                    onComplete(true)
                }
            }
        }

        override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
            if (stack.size > 1) {
                stack = stack.dropLast(n = 1)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }

        override fun popTo(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
            val newStack = stack.dropLastWhile { it != route }.ifEmpty { stack }
            val isSuccess = newStack.size < stack.size
            stack = newStack
            onComplete(isSuccess)
        }

        override fun defaultCompletionHandler(isSuccess: Boolean, errorMessage: String) {
            if (!isSuccess) errors += errorMessage
        }

        override fun replaceCurrent(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) = Unit

        override fun replaceAll(vararg routes: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) = Unit

        override fun popTo(routeClass: KClass<out AppRoute>, onComplete: (isSuccess: Boolean) -> Unit) = Unit
    }
}