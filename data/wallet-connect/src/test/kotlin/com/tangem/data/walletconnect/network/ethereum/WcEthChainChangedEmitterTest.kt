package com.tangem.data.walletconnect.network.ethereum

import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.sdk.WcSdk
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcEthChainChangedEmitterTest {

    private val wcSdk: WcSdk = mockk()


    @BeforeEach
    fun resetMocks() {
        clearMocks(wcSdk)
        coEvery { wcSdk.emitSessionEvent(any()) } returns Wallet.Params.SessionEmit(
            topic = TOPIC,
            event = Wallet.Model.SessionEvent(name = "chainChanged", data = "0x1"),
            chainId = POLYGON_CAIP2.raw,
        ).right()
    }

    @Test
    fun `GIVEN session already carries the chain WHEN emit THEN chainChanged is sent as hex`() = runTest {
        // Arrange
        every { wcSdk.sessionCarriesChain(TOPIC, POLYGON_CAIP2.raw) } returns true
        val paramsSlot = slot<Wallet.Params.SessionEmit>()
        coEvery { wcSdk.emitSessionEvent(capture(paramsSlot)) } returns mockk<Wallet.Params.SessionEmit>().right()

        // Act
        emitter().emit(topic = TOPIC, chainId = POLYGON_CAIP2)

        // Assert
        val params = paramsSlot.captured
        assertEquals(TOPIC, params.topic)
        assertEquals(POLYGON_CAIP2.raw, params.chainId)
        assertEquals("chainChanged", params.event.name)
        assertEquals("0x89", params.event.data)
    }

    @Test
    fun `GIVEN the chain is already carried WHEN emit THEN chainChanged is sent`() = runTest {
        // Arrange
        every { wcSdk.sessionCarriesChain(TOPIC, POLYGON_CAIP2.raw) } returns true

        // Act
        emitter().emit(topic = TOPIC, chainId = POLYGON_CAIP2)

        // Assert
        coVerify(exactly = 1) { wcSdk.emitSessionEvent(any()) }
    }

    @Test
    fun `GIVEN chain appears after the session update WHEN emit THEN chainChanged is sent`() = runTest {
        // Arrange
        every { wcSdk.sessionCarriesChain(TOPIC, POLYGON_CAIP2.raw) } returnsMany listOf(false, false, true)

        // Act
        emitter().emit(topic = TOPIC, chainId = POLYGON_CAIP2)

        // Assert
        coVerify(exactly = 1) { wcSdk.emitSessionEvent(any()) }
    }

    @Test
    fun `GIVEN chain never appears WHEN emit THEN nothing is sent`() = runTest {
        // Arrange
        every { wcSdk.sessionCarriesChain(TOPIC, POLYGON_CAIP2.raw) } returns false

        // Act
        emitter().emit(topic = TOPIC, chainId = POLYGON_CAIP2)

        // Assert
        coVerify(exactly = 0) { wcSdk.emitSessionEvent(any()) }
    }

    @Test
    fun `GIVEN the session cannot be read WHEN emit THEN nothing is sent`() = runTest {
        // Arrange
        every { wcSdk.sessionCarriesChain(TOPIC, POLYGON_CAIP2.raw) } returns null

        // Act
        emitter().emit(topic = TOPIC, chainId = POLYGON_CAIP2)

        // Assert
        coVerify(exactly = 0) { wcSdk.emitSessionEvent(any()) }
    }

    @Test
    fun `GIVEN non-numeric chain reference WHEN emit THEN the session is not even read`() = runTest {
        // Act
        emitter().emit(topic = TOPIC, chainId = CAIP2(namespace = "eip155", reference = "not-a-number"))

        // Assert
        coVerify(exactly = 0) { wcSdk.emitSessionEvent(any()) }
        verify(exactly = 0) { wcSdk.sessionCarriesChain(any(), any()) }
    }

    private fun TestScope.emitter() = WcEthChainChangedEmitter(
        wcSdk = wcSdk,
        dispatchers = createTestingCoroutineDispatcherProvider(),
    )


    private companion object {
        const val TOPIC = "topic"
        val POLYGON_CAIP2 = CAIP2(namespace = "eip155", reference = "137")
    }
}