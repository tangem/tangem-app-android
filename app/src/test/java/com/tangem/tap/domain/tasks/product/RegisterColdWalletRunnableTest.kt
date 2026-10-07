package com.tangem.tap.domain.tasks.product

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.lib.auth.session.WalletNonce
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RegisterColdWalletRunnableTest {

    private val launcher: WalletRegistrationLauncher = mockk()
    private val inner: CardSessionRunnable<String> = mockk()
    private val card: Card = mockk()
    private val nonce: WalletNonce = mockk()
    private val session: CardSession = mockk {
        every { environment.card } returns card
    }

    @BeforeEach
    fun setup() {
        clearMocks(launcher, inner)
        every { session.environment.card } returns card
        coEvery { launcher.registerColdInSession(any(), any(), any(), any()) } just Runs
        stubInnerPrepare()
    }

    @Test
    fun `GIVEN nonce ready WHEN prepare then run THEN registers cold and forwards result`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        stubInnerRun(CompletionResult.Success("ok"))
        coEvery { launcher.prefetchColdNonce() } returns nonce

        // Act
        val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(inner, launcher))

        // Assert
        coVerify(exactly = 1) { launcher.registerColdInSession(session, card, nonce, false) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN forced registration and no nonce WHEN prepare then run THEN still registers forced`() = runTest {
        // Arrange — the forced path must reach the launcher without a nonce to drop the stale marker.
        every { session.scope } returns unconfinedScope()
        stubInnerRun(CompletionResult.Success("ok"))
        coEvery { launcher.prefetchColdNonce() } returns null

        // Act
        val forwarded = prepareThenRunAndCapture(
            RegisterColdWalletRunnable(inner, launcher, shouldForceRegistration = true),
        )

        // Assert
        coVerify(exactly = 1) { launcher.registerColdInSession(session, card, null, true) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN forced registration and nonce ready WHEN prepare then run THEN registers forced with nonce`() =
        runTest {
            // Arrange
            every { session.scope } returns unconfinedScope()
            stubInnerRun(CompletionResult.Success("ok"))
            coEvery { launcher.prefetchColdNonce() } returns nonce

            // Act
            prepareThenRunAndCapture(RegisterColdWalletRunnable(inner, launcher, shouldForceRegistration = true))

            // Assert
            coVerify(exactly = 1) { launcher.registerColdInSession(session, card, nonce, true) }
        }

    @Test
    fun `GIVEN forced registration and inner fails WHEN run THEN nothing registered`() = runTest {
        // Arrange — the marker is dropped only once the import itself has succeeded.
        every { session.scope } returns unconfinedScope()
        stubInnerRun(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        coEvery { launcher.prefetchColdNonce() } returns nonce

        // Act
        prepareThenRunAndCapture(RegisterColdWalletRunnable(inner, launcher, shouldForceRegistration = true))

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN inner fails WHEN run THEN forwards failure without registering`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        stubInnerRun(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        coEvery { launcher.prefetchColdNonce() } returns nonce

        // Act
        val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(inner, launcher))

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any(), any()) }
        assertThat(forwarded).isInstanceOf(CompletionResult.Failure::class.java)
    }

    @Test
    fun `GIVEN session has no card WHEN run THEN forwards result without registering`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        every { session.environment.card } returns null
        stubInnerRun(CompletionResult.Success("ok"))
        coEvery { launcher.prefetchColdNonce() } returns nonce

        // Act
        val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(inner, launcher))

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any(), any()) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN registration throws WHEN run THEN the result is still forwarded`() = runTest {
        // Arrange — registration must never break the wrapped operation.
        every { session.scope } returns unconfinedScope()
        stubInnerRun(CompletionResult.Success("ok"))
        coEvery { launcher.prefetchColdNonce() } returns nonce
        coEvery { launcher.registerColdInSession(any(), any(), any(), any()) } throws IllegalStateException("boom")

        // Act
        val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(inner, launcher))

        // Assert
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN nonce prefetch throws WHEN prepare THEN result forwarded without registering`() = runTest {
        // Arrange — a throwing prefetch must not escape prepare or block forwarding.
        every { session.scope } returns unconfinedScope()
        stubInnerRun(CompletionResult.Success("ok"))
        coEvery { launcher.prefetchColdNonce() } throws IllegalStateException("boom")

        // Act
        val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(inner, launcher))

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any(), any()) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN inner prepare fails WHEN prepare THEN failure forwarded without prefetching`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        stubInnerPrepare(CompletionResult.Failure(TangemSdkError.UserCancelled()))

        // Act
        var prepared: CompletionResult<Unit>? = null
        RegisterColdWalletRunnable(inner, launcher).prepare(session) { prepared = it }
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { launcher.prefetchColdNonce() }
        assertThat(prepared).isInstanceOf(CompletionResult.Failure::class.java)
    }

    @Test
    fun `GIVEN host operation and nonce ready WHEN run THEN registers mid-operation before it completes`() =
        runTest {
            // Arrange
            every { session.scope } returns unconfinedScope()
            coEvery { launcher.prefetchColdNonce() } returns nonce
            val events = mutableListOf<String>()
            coEvery { launcher.registerColdInSession(any(), any(), any(), any()) } answers { events += "registered" }
            val host = FakeRegistrationHost(onRest = { events += "rest of operation" })

            // Act
            val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(host, launcher))

            // Assert
            coVerify(exactly = 1) { launcher.registerColdInSession(session, card, nonce, false) }
            assertThat(events).containsExactly("registered", "rest of operation").inOrder()
            assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
        }

    @Test
    fun `GIVEN host operation and registration throws WHEN run THEN the operation still proceeds`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        coEvery { launcher.prefetchColdNonce() } returns nonce
        coEvery { launcher.registerColdInSession(any(), any(), any(), any()) } throws IllegalStateException("boom")
        var hasProceeded = false
        val host = FakeRegistrationHost(onRest = { hasProceeded = true })

        // Act
        val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(host, launcher))

        // Assert
        assertThat(hasProceeded).isTrue()
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN host operation and no nonce WHEN run THEN proceeds without registering`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        coEvery { launcher.prefetchColdNonce() } returns null
        var hasProceeded = false
        val host = FakeRegistrationHost(onRest = { hasProceeded = true })

        // Act
        val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(host, launcher))

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any(), any()) }
        assertThat(hasProceeded).isTrue()
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN host operation fails after registration WHEN run THEN failure forwarded and registered once`() =
        runTest {
            // Arrange
            every { session.scope } returns unconfinedScope()
            coEvery { launcher.prefetchColdNonce() } returns nonce
            val host = FakeRegistrationHost(result = CompletionResult.Failure(TangemSdkError.UserCancelled()))

            // Act
            val forwarded = prepareThenRunAndCapture(RegisterColdWalletRunnable(host, launcher))

            // Assert
            coVerify(exactly = 1) { launcher.registerColdInSession(any(), any(), any(), any()) }
            assertThat(forwarded).isInstanceOf(CompletionResult.Failure::class.java)
        }

    /** Runs the handed registration first, then the rest of the operation, like `ScanProductTask`. */
    private class FakeRegistrationHost(
        private val result: CompletionResult<String> = CompletionResult.Success("ok"),
        private val onRest: () -> Unit = {},
    ) : CardSessionRunnable<String>, ColdWalletRegistrationHost {

        override var coldWalletRegistration: InSessionColdWalletRegistration? = null

        override fun run(session: CardSession, callback: (result: CompletionResult<String>) -> Unit) {
            val proceed = {
                onRest()
                callback(result)
            }
            coldWalletRegistration?.register(session, proceed) ?: proceed()
        }
    }

    /** Makes [inner] invoke its run callback synchronously with [result]. */
    private fun stubInnerRun(result: CompletionResult<String>) {
        every { inner.run(any(), any()) } answers {
            secondArg<(CompletionResult<String>) -> Unit>().invoke(result)
        }
    }

    /** Makes [inner] invoke its prepare callback synchronously with [result]. */
    private fun stubInnerPrepare(result: CompletionResult<Unit> = CompletionResult.Success(Unit)) {
        every { inner.prepare(any(), any()) } answers {
            secondArg<(CompletionResult<Unit>) -> Unit>().invoke(result)
        }
    }

    // Unconfined dispatcher runs the session.scope launches inline (mocks never suspend unless told to),
    // so the prefetch and the forwarding callback resolve on the shared test scheduler.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.unconfinedScope() = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    private fun TestScope.prepareThenRunAndCapture(
        runnable: RegisterColdWalletRunnable<String>,
    ): CompletionResult<String> {
        runnable.prepare(session) {}
        advanceUntilIdle()
        var captured: CompletionResult<String>? = null
        runnable.run(session) { captured = it }
        advanceUntilIdle()
        return requireNotNull(captured) { "callback was not invoked" }
    }
}