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
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        coEvery { launcher.registerColdInSession(any(), any(), any()) } just Runs
    }

    @Test
    fun `GIVEN nonce ready and inner succeeds WHEN run THEN registers cold and forwards result`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        stubInner(CompletionResult.Success("ok"))
        coEvery { launcher.requestColdNonce(card, checkRegistered = true) } returns nonce
        val runnable = RegisterColdWalletRunnable(inner, launcher)

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        coVerify(exactly = 1) { launcher.registerColdInSession(session, card, nonce) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN nonce slower than the card command WHEN run THEN skips registration`() = runTest {
        // Arrange — the nonce is still loading when the wrapped command completes.
        every { session.scope } returns unconfinedScope()
        stubInner(CompletionResult.Success("ok"))
        coEvery { launcher.requestColdNonce(card, checkRegistered = true) } coAnswers {
            delay(timeMillis = 10_000)
            nonce
        }
        val runnable = RegisterColdWalletRunnable(inner, launcher)

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any()) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN inner fails WHEN run THEN forwards failure without registering`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        stubInner(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        coEvery { launcher.requestColdNonce(card, checkRegistered = true) } returns nonce
        val runnable = RegisterColdWalletRunnable(inner, launcher)

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any()) }
        assertThat(forwarded).isInstanceOf(CompletionResult.Failure::class.java)
    }

    @Test
    fun `GIVEN session has no card WHEN run THEN forwards result without prefetching or registering`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        every { session.environment.card } returns null
        stubInner(CompletionResult.Success("ok"))
        val runnable = RegisterColdWalletRunnable(inner, launcher)

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        coVerify(exactly = 0) { launcher.requestColdNonce(any(), any()) }
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any()) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN registration throws WHEN run THEN the result is still forwarded`() = runTest {
        // Arrange — registration must never break the wrapped operation.
        every { session.scope } returns unconfinedScope()
        stubInner(CompletionResult.Success("ok"))
        coEvery { launcher.requestColdNonce(card, checkRegistered = true) } returns nonce
        coEvery { launcher.registerColdInSession(any(), any(), any()) } throws IllegalStateException("boom")
        val runnable = RegisterColdWalletRunnable(inner, launcher)

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN nonce prefetch throws WHEN run THEN result is still forwarded without registering`() = runTest {
        // Arrange — a throwing prefetch completes the Deferred exceptionally; it must not escape run().
        every { session.scope } returns unconfinedScope()
        stubInner(CompletionResult.Success("ok"))
        coEvery { launcher.requestColdNonce(card, checkRegistered = true) } throws IllegalStateException("mapping failed")
        val runnable = RegisterColdWalletRunnable(inner, launcher)

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any(), any()) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN a wallet-creating inner WHEN run THEN prefetch skips the already-registered gate`() = runTest {
        // Arrange — a create/import task replaces the first wallet, so the preflight id must not gate.
        every { session.scope } returns unconfinedScope()
        val createInner = mockk<CreateProductWalletTask>()
        every { createInner.run(any(), any()) } just Runs
        coEvery { launcher.requestColdNonce(any(), any()) } returns null
        val runnable = RegisterColdWalletRunnable(createInner, launcher)

        // Act
        runnable.run(session) {}

        // Assert
        coVerify { launcher.requestColdNonce(card, checkRegistered = false) }
    }

    /** Makes [inner] invoke its callback synchronously with [result]. */
    private fun stubInner(result: CompletionResult<String>) {
        every { inner.run(any(), any()) } answers {
            secondArg<(CompletionResult<String>) -> Unit>().invoke(result)
        }
    }

    // Unconfined dispatcher runs the session.scope launches inline (registration mock never suspends),
    // so the forwarding callback fires synchronously within run().
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.unconfinedScope() = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    private fun runAndCapture(runnable: RegisterColdWalletRunnable<String>): CompletionResult<String> {
        var captured: CompletionResult<String>? = null
        runnable.run(session) { captured = it }
        return requireNotNull(captured) { "callback was not invoked" }
    }
}