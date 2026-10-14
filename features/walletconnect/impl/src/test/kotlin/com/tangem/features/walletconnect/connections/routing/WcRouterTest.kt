package com.tangem.features.walletconnect.connections.routing

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.navigation.Route
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WcRouterTest {

    private val slotNavigation: SlotNavigation<WcInnerRoute> = mockk()

    private val results = mutableListOf<Boolean>()
    private val onComplete: (Boolean) -> Unit = { results += it }

    private val router = WcRouter(slotNavigation)

    @BeforeEach
    fun setUp() {
        clearMocks(slotNavigation)
        results.clear()

        every { slotNavigation.navigate(any(), any()) } answers {
            secondArg<(WcInnerRoute?, WcInnerRoute?) -> Unit>().invoke(null, null)
        }
    }

    @Test
    fun `GIVEN active slot WHEN pop THEN slot is dismissed and completion is successful`() {
        // Act
        router.pop(onComplete)

        // Assert
        verify(exactly = 1) { slotNavigation.navigate(any(), any()) }
        assertThat(results).containsExactly(true)
    }

    @Test
    fun `GIVEN unsupported operations WHEN invoked THEN each reports unsuccessful completion`() {
        // Act
        router.replaceCurrent(OtherRoute, onComplete)
        router.replaceAll(OtherRoute, onComplete = onComplete)
        router.popTo(OtherRoute, onComplete)
        router.popTo(OtherRoute::class, onComplete)

        // Assert
        assertThat(results).containsExactly(false, false, false, false)
        verify(exactly = 0) { slotNavigation.navigate(any(), any()) }
    }

    @Test
    fun `GIVEN route of another feature WHEN push THEN it is rejected`() {
        // Act
        router.push(OtherRoute, onComplete)

        // Assert
        assertThat(results).containsExactly(false)
        verify(exactly = 0) { slotNavigation.navigate(any(), any()) }
    }

    private data object OtherRoute : Route
}