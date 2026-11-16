package com.tangem.core.decompose.navigation

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class ZoomTransitionsTest {

    private data class ScreenRoute(val id: Int) : Route

    @Test
    fun `GIVEN a route pushed zooming WHEN it is claimed THEN its source key is returned`() {
        // Arrange
        val route = ScreenRoute(id = 1)
        val router: Router = mockk(relaxed = true)

        // Act
        router.pushZooming(route = route, sourceKey = "card")

        // Assert
        verify(exactly = 1) { router.push(route = route, onComplete = any()) }
        assertThat(ZoomTransitions.claim(route)).isEqualTo("card")
    }

    @Test
    fun `GIVEN a claimed route WHEN it is claimed again THEN nothing is returned`() {
        // Arrange
        val route = ScreenRoute(id = 2)
        ZoomTransitions.register(route = route, sourceKey = "card")
        ZoomTransitions.claim(route)

        // Act
        val sourceKey = ZoomTransitions.claim(route)

        // Assert
        assertThat(sourceKey).isNull()
    }

    @Test
    fun `GIVEN a route registered WHEN an equal but different instance is claimed THEN nothing is returned`() {
        // Arrange
        val route = ScreenRoute(id = 3)
        ZoomTransitions.register(route = route, sourceKey = "card")

        // Act
        val sourceKey = ZoomTransitions.claim(ScreenRoute(id = 3))

        // Assert
        assertThat(sourceKey).isNull()
        assertThat(ZoomTransitions.claim(route)).isEqualTo("card")
    }

    @Test
    fun `GIVEN more pending routes than the cap WHEN the oldest is claimed THEN it was dropped`() {
        // Arrange
        val routes = (100 until 109).map(::ScreenRoute)
        routes.forEach { route -> ZoomTransitions.register(route = route, sourceKey = "card_${route.id}") }

        // Act
        val oldest = ZoomTransitions.claim(routes.first())

        // Assert
        assertThat(oldest).isNull()
        assertThat(ZoomTransitions.claim(routes.last())).isEqualTo("card_108")
    }
}