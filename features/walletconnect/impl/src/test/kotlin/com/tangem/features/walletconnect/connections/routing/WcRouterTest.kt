package com.tangem.features.walletconnect.connections.routing

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class WcRouterTest {

    private val fallBackRouter = RecordingRouter()
    private val router = WcRouter(slotNavigation = SlotNavigation(), fallBackRouter = fallBackRouter)

    @BeforeEach
    fun setUp() {
        fallBackRouter.calls.clear()
    }

    @Test
    fun `GIVEN WalletConnect route WHEN push THEN slot opens and fallback router is not used`() {
        // Arrange
        val results = mutableListOf<Boolean>()

        // Act
        router.push(WcInnerRoute.UnsupportedMethodAlert) { results += it }

        // Assert
        assertThat(results).containsExactly(true)
        assertThat(fallBackRouter.calls).isEmpty()
    }

    @Test
    fun `GIVEN outer routes WHEN navigating THEN every operation goes to fallback router`() = runBlocking {
        // Act
        router.push(OuterRoute)
        router.replaceCurrent(OuterRoute)
        router.replaceAll(OuterRoute)
        router.popTo(OuterRoute)
        router.popTo(OuterRoute::class)
        router.popToMatching(OuterRoute::class, predicate = { true })
        router.navigate(transformer = { it })
        router.contains(OuterRoute)

        // Assert
        assertThat(fallBackRouter.calls).containsExactly(
            "push", "replaceCurrent", "replaceAll", "popTo", "popToClass", "popToMatching", "navigate", "contains",
        ).inOrder()
    }

    @Test
    fun `GIVEN WalletConnect routes WHEN navigating THEN fallback router is not used`() = runBlocking {
        // Act
        router.replaceCurrent(WcInnerRoute.UnsupportedMethodAlert)
        router.popTo(WcInnerRoute.UnsupportedMethodAlert)
        router.popTo(WcInnerRoute.UnsupportedMethodAlert::class)
        val isContained = router.contains(WcInnerRoute.UnsupportedMethodAlert)

        // Assert
        assertThat(isContained).isFalse()
        assertThat(fallBackRouter.calls).isEmpty()
    }

    private data object OuterRoute : Route

    private class RecordingRouter : Router {

        val calls = mutableListOf<String>()

        override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
            calls += "push"
        }

        override fun replaceCurrent(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
            calls += "replaceCurrent"
        }

        override fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit) {
            calls += "replaceAll"
        }

        override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
            calls += "pop"
        }

        override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
            calls += "popTo"
        }

        override fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit) {
            calls += "popToClass"
        }

        override fun <R : Route> popToMatching(
            routeClass: KClass<R>,
            predicate: (R) -> Boolean,
            onComplete: (isSuccess: Boolean) -> Unit,
        ) {
            calls += "popToMatching"
        }

        override fun navigate(
            transformer: (stack: List<Route>) -> List<Route>,
            onComplete: (isSuccess: Boolean) -> Unit,
        ) {
            calls += "navigate"
        }

        override suspend fun contains(route: Route): Boolean {
            calls += "contains"
            return false
        }
    }
}