package com.tangem.data.wallets.signing

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeriveKeyInSessionTaskTest {

    private val derivationsRepository: DerivationsRepository = mockk(relaxUnitFun = true)
    private val session: CardSession = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(derivationsRepository)
    }

    @Test
    fun `GIVEN the caller is already gone WHEN the card is tapped THEN the session closes as cancelled`() = runTest {
        // Arrange
        val cancelledScope = CoroutineScope(coroutineContext + Job().apply { cancel() })
        var isDerived = false
        var sessionResult: CompletionResult<Unit>? = null
        val task = DeriveKeyInSessionTask(
            userWalletId = UserWalletId("0A"),
            request = DeriveKeyInSessionTask.Request(seedKey = SEED_KEY, path = PATH),
            derivationsRepository = derivationsRepository,
            deriveInSession = { _, _ ->
                isDerived = true
                CompletionResult.Success(DERIVED_KEY)
            },
            scope = cancelledScope,
            onDerived = { _, _, sessionCallback -> sessionCallback.close(CompletionResult.Success(Unit)) },
        )

        // Act
        task.run(session) { sessionResult = it }
        yield()

        // Assert
        assertThat((sessionResult as CompletionResult.Failure).error)
            .isInstanceOf(TangemSdkError.UserCancelled::class.java)
        assertThat(isDerived).isFalse()
        coVerify(exactly = 0) { derivationsRepository.storeDerivedKeys(any(), any()) }
    }

    private companion object {
        val PATH = DerivationPath("m/44'/60'/999899'/0/0")
        val SEED_KEY = "02AABB".hexToBytes()
        val DERIVED_KEY = ExtendedPublicKey(
            publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
            chainCode = "00".repeat(32).hexToBytes(),
        )
    }
}