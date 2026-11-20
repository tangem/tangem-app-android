package com.tangem.tap.domain.tasks

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.TangemSdkError
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PreparingSignHashesTaskTest {

    private val session: CardSession = mockk()
    private val publicKey: Wallet.PublicKey = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(session)
    }

    @Test
    fun `GIVEN hashes provider fails WHEN run THEN finishes as user cancelled`() = runTest {
        // Arrange
        every { session.scope } returns unconfinedScope()
        stubSecureChannel(CompletionResult.Success(Unit))
        val task = PreparingSignHashesTask(
            hashesProvider = { error("Blockhash request failed") },
            publicKey = publicKey,
            pairWalletPublicKey = null,
        )

        // Act
        val results = runAndCapture(task)

        // Assert
        assertThat(results).hasSize(1)
        assertThat((results.single() as CompletionResult.Failure).error)
            .isInstanceOf(TangemSdkError.UserCancelled::class.java)
    }

    @Test
    fun `GIVEN secure channel fails WHEN run THEN forwards its error without preparing`() = runTest {
        // Arrange
        val channelError = TangemSdkError.AccessCodeRequired()
        stubSecureChannel(CompletionResult.Failure(channelError))
        var isProviderCalled = false
        val task = PreparingSignHashesTask(
            hashesProvider = {
                isProviderCalled = true
                emptyList()
            },
            publicKey = publicKey,
            pairWalletPublicKey = null,
        )

        // Act
        val results = runAndCapture(task)

        // Assert
        assertThat(results).hasSize(1)
        assertThat((results.single() as CompletionResult.Failure).error).isSameInstanceAs(channelError)
        assertThat(isProviderCalled).isFalse()
    }

    private fun stubSecureChannel(result: CompletionResult<Unit>) {
        every { session.establishEncryptionIfNeeded(any(), any(), any()) } answers {
            thirdArg<(CompletionResult<Unit>) -> Unit>().invoke(result)
        }
    }

    private fun TestScope.unconfinedScope() = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    private fun TestScope.runAndCapture(task: PreparingSignHashesTask): List<CompletionResult<TangemSignHashesResponse>> {
        val results = mutableListOf<CompletionResult<TangemSignHashesResponse>>()
        task.run(session) { results += it }
        advanceUntilIdle()
        return results
    }
}