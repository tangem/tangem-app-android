package com.tangem.tap.domain.tasks

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DerivePublicKeysTaskTest {

    private val inner: CardSessionRunnable<DerivationTaskResponse> = mockk()
    private val session: CardSession = mockk()

    private val walletKey: ByteArrayKey = byteArrayOf(1, 2, 3).toMapKey()
    private val anotherWalletKey: ByteArrayKey = byteArrayOf(4, 5, 6).toMapKey()
    private val path: DerivationPath = mockk()
    private val derivedKey: ExtendedPublicKey = mockk()

    private val derivations = mapOf(walletKey to listOf(path), anotherWalletKey to listOf(path))

    @BeforeEach
    fun resetMocks() {
        clearMocks(inner)
    }

    @Test
    fun `GIVEN card derived the requested keys WHEN run THEN forwards success`() {
        // Arrange
        val response = DerivationTaskResponse(
            entries = mapOf(
                walletKey to ExtendedPublicKeysMap(mapOf(path to derivedKey)),
                anotherWalletKey to ExtendedPublicKeysMap(mapOf(path to derivedKey)),
            ),
        )
        stubInner(CompletionResult.Success(response))

        // Act
        val actual = run(DerivePublicKeysTask(derivations, inner))

        // Assert
        assertThat((actual as CompletionResult.Success).data).isSameInstanceAs(response)
    }

    @Test
    fun `GIVEN card derived keys for one of two wallets WHEN run THEN forwards partial success`() {
        // Arrange
        val response = DerivationTaskResponse(
            entries = mapOf(
                walletKey to ExtendedPublicKeysMap(mapOf(path to derivedKey)),
                anotherWalletKey to ExtendedPublicKeysMap(emptyMap()),
            ),
        )
        stubInner(CompletionResult.Success(response))

        // Act
        val actual = run(DerivePublicKeysTask(derivations, inner))

        // Assert
        assertThat((actual as CompletionResult.Success).data).isSameInstanceAs(response)
    }

    @Test
    fun `GIVEN card derived nothing WHEN run THEN fails with wallet not found`() {
        // Arrange
        val response = DerivationTaskResponse(
            entries = mapOf(
                walletKey to ExtendedPublicKeysMap(emptyMap()),
                anotherWalletKey to ExtendedPublicKeysMap(emptyMap()),
            ),
        )
        stubInner(CompletionResult.Success(response))

        // Act
        val actual = run(DerivePublicKeysTask(derivations, inner))

        // Assert
        assertThat((actual as CompletionResult.Failure).error).isInstanceOf(TangemSdkError.WalletNotFound::class.java)
    }

    @Test
    fun `GIVEN card returned no entries at all WHEN run THEN fails with wallet not found`() {
        // Arrange
        stubInner(CompletionResult.Success(DerivationTaskResponse(entries = emptyMap())))

        // Act
        val actual = run(DerivePublicKeysTask(derivations, inner))

        // Assert
        assertThat((actual as CompletionResult.Failure).error).isInstanceOf(TangemSdkError.WalletNotFound::class.java)
    }

    @Test
    fun `GIVEN nothing to derive WHEN run THEN forwards empty success`() {
        // Arrange
        val response = DerivationTaskResponse(entries = emptyMap())
        stubInner(CompletionResult.Success(response))

        // Act
        val actual = run(DerivePublicKeysTask(derivations = emptyMap(), inner = inner))

        // Assert
        assertThat((actual as CompletionResult.Success).data).isSameInstanceAs(response)
    }

    @Test
    fun `GIVEN derivation failed WHEN run THEN forwards failure`() {
        // Arrange
        val error = TangemSdkError.UserCancelled()
        stubInner(CompletionResult.Failure(error))

        // Act
        val actual = run(DerivePublicKeysTask(derivations, inner))

        // Assert
        assertThat((actual as CompletionResult.Failure).error).isSameInstanceAs(error)
    }

    private fun stubInner(result: CompletionResult<DerivationTaskResponse>) {
        every { inner.run(session, any()) } answers {
            secondArg<CompletionCallback<DerivationTaskResponse>>().invoke(result)
        }
    }

    private fun run(task: DerivePublicKeysTask): CompletionResult<DerivationTaskResponse> {
        var forwarded: CompletionResult<DerivationTaskResponse>? = null
        task.run(session) { forwarded = it }

        return requireNotNull(forwarded) { "The task did not complete" }
    }
}