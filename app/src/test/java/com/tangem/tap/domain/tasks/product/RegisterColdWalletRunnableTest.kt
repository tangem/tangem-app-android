package com.tangem.tap.domain.tasks.product

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RegisterColdWalletRunnableTest {

    private val launcher: WalletRegistrationLauncher = mockk()
    private val inner: CardSessionRunnable<String> = mockk()
    private val card: Card = mockk()
    private val session: CardSession = mockk {
        every { environment.card } returns card
    }

    @BeforeEach
    fun setup() {
        clearMocks(launcher, inner)
        every { session.environment.card } returns card
        coEvery { launcher.registerColdInSession(any(), any<Card>()) } just Runs
    }

    @Test
    fun `GIVEN inner succeeds and card present WHEN run THEN registers cold and forwards result`() = runTest {
        // Arrange
        stubInner(CompletionResult.Success("ok"))
        val runnable = RegisterColdWalletRunnable(inner, launcher, sessionScope = unconfinedScope())

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        coVerify(exactly = 1) { launcher.registerColdInSession(session, card) }
        assertThat(forwarded).isInstanceOf(CompletionResult.Success::class.java)
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN inner fails WHEN run THEN forwards failure without registering`() = runTest {
        // Arrange
        stubInner(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        val runnable = RegisterColdWalletRunnable(inner, launcher, sessionScope = unconfinedScope())

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any<Card>()) }
        assertThat(forwarded).isInstanceOf(CompletionResult.Failure::class.java)
    }

    @Test
    fun `GIVEN inner succeeds but session has no card WHEN run THEN forwards result without registering`() = runTest {
        // Arrange
        every { session.environment.card } returns null
        stubInner(CompletionResult.Success("ok"))
        val runnable = RegisterColdWalletRunnable(inner, launcher, sessionScope = unconfinedScope())

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        coVerify(exactly = 0) { launcher.registerColdInSession(any(), any<Card>()) }
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    @Test
    fun `GIVEN registration throws WHEN run THEN the result is still forwarded`() = runTest {
        // Arrange — registration must never break the wrapped operation.
        coEvery { launcher.registerColdInSession(any(), any<Card>()) } throws IllegalStateException("boom")
        stubInner(CompletionResult.Success("ok"))
        val runnable = RegisterColdWalletRunnable(inner, launcher, sessionScope = unconfinedScope())

        // Act
        val forwarded = runAndCapture(runnable)

        // Assert
        assertThat((forwarded as CompletionResult.Success).data).isEqualTo("ok")
    }

    /** Makes [inner] invoke its callback synchronously with [result]. */
    private fun stubInner(result: CompletionResult<String>) {
        every { inner.run(any(), any()) } answers {
            secondArg<(CompletionResult<String>) -> Unit>().invoke(result)
        }
    }

    // Unconfined dispatcher runs the sessionScope launch inline (registration mock never suspends),
    // so the forwarding callback fires synchronously within run().
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.unconfinedScope() = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    private fun runAndCapture(runnable: RegisterColdWalletRunnable<String>): CompletionResult<String> {
        var captured: CompletionResult<String>? = null
        runnable.run(session) { captured = it }
        return requireNotNull(captured) { "callback was not invoked" }
    }
}