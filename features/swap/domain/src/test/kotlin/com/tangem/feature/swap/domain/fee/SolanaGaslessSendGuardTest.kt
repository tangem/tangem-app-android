package com.tangem.feature.swap.domain.fee

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SolanaGaslessSendGuardTest {

    private val walletId = UserWalletId("011")
    private val otherWalletId = UserWalletId("022")
    private var nowMillis = 0L

    // Recreated per test: the guard keeps per-wallet state
    private lateinit var sut: SolanaGaslessSendGuard

    @BeforeEach
    fun setup() {
        nowMillis = 0L
        sut = SolanaGaslessSendGuard { nowMillis }
    }

    @Test
    fun `GIVEN no send yet WHEN isBlocked THEN false`() {
        // Act
        val actual = sut.isBlocked(walletId)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN a send in flight WHEN isBlocked and guard again THEN blocked and second send not run`() = runTest {
        // Arrange
        var isBlockedWhileSending = false
        var secondSend: SwapTransactionState? = txSent()
        var isSecondSendRun = false

        // Act
        sut.guard(walletId) {
            isBlockedWhileSending = sut.isBlocked(walletId)
            secondSend = sut.guard(walletId) {
                isSecondSendRun = true
                txSent()
            }
            txSent()
        }

        // Assert
        assertThat(isBlockedWhileSending).isTrue()
        assertThat(secondSend).isNull()
        assertThat(isSecondSendRun).isFalse()
        assertThat(sut.isBlocked(otherWalletId)).isFalse()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN send result WHEN guarded send ends THEN only a refusal holds the wallet`(model: ResultModel) = runTest {
        // Act
        sut.guard(walletId) { model.result }

        // Assert
        assertThat(sut.isBlocked(walletId)).isEqualTo(model.isHeld)
    }

    @Test
    fun `GIVEN refusal WHEN its hold has passed THEN not blocked`() = runTest {
        // Arrange
        sut.guard(walletId) { gaslessError(SolanaGaslessError.Rejected(code = null)) }

        // Act
        nowMillis += 59_999
        val isBlockedWithinHold = sut.isBlocked(walletId)
        nowMillis += 1
        val isBlockedAfterHold = sut.isBlocked(walletId)

        // Assert
        assertThat(isBlockedWithinHold).isTrue()
        assertThat(isBlockedAfterHold).isFalse()
        assertThat(sut.isBlocked(otherWalletId)).isFalse()
    }

    @Test
    fun `GIVEN refusal held WHEN a later send succeeds THEN hold released`() = runTest {
        // Arrange
        sut.guard(walletId) { gaslessError(SolanaGaslessError.RateLimited(isPerWallet = true)) }

        // Act
        sut.guard(walletId) { txSent() }

        // Assert
        assertThat(sut.isBlocked(walletId)).isFalse()
    }

    @Test
    fun `GIVEN send throws WHEN guarded THEN rethrown and wallet held`() = runTest {
        // Arrange
        val failure = IllegalStateException("storing the sent swap failed")

        // Act
        val thrown = runCatching { sut.guard(walletId) { throw failure } }.exceptionOrNull()

        // Assert
        assertThat(thrown).isEqualTo(failure)
        assertThat(sut.isBlocked(walletId)).isTrue()
    }

    data class ResultModel(val result: SwapTransactionState, val isHeld: Boolean)

    private fun provideTestModels() = listOf(
        ResultModel(result = txSent(), isHeld = false),
        ResultModel(result = gaslessError(SolanaGaslessError.Rejected(code = "X")), isHeld = true),
        ResultModel(result = gaslessError(SolanaGaslessError.RateLimited(isPerWallet = false)), isHeld = true),
        ResultModel(result = gaslessError(SolanaGaslessError.MaybeSent(signature = null)), isHeld = true),
        ResultModel(
            result = gaslessError(SolanaGaslessError.ServiceUnavailable(code = null, isSponsorshipUnavailable = true)),
            isHeld = true,
        ),
        ResultModel(result = gaslessError(SolanaGaslessError.ServiceUnavailable(code = null)), isHeld = false),
        ResultModel(result = gaslessError(SolanaGaslessError.BlockhashExpired), isHeld = false),
        ResultModel(result = gaslessError(SolanaGaslessError.Unknown(message = null)), isHeld = false),
        ResultModel(
            result = SwapTransactionState.Error.TransactionError(SendTransactionError.UserCancelledError),
            isHeld = false,
        ),
    )

    private fun gaslessError(error: SolanaGaslessError) = SwapTransactionState.Error.SolanaGaslessError(error)

    private fun txSent(): SwapTransactionState = mockk<SwapTransactionState.TxSent>()
}