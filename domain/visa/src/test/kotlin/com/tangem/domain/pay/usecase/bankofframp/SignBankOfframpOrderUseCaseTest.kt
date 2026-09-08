package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpSignResult
import com.tangem.domain.pay.repository.BankOfframpRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SignBankOfframpOrderUseCaseTest {

    private val repository: BankOfframpRepository = mockk()
    private val authDataSource: TangemPayAuthDataSource = mockk()
    private val userWallet: UserWallet = mockk()
    private val useCase = SignBankOfframpOrderUseCase(repository = repository, authDataSource = authDataSource)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, authDataSource, userWallet)
        every { userWallet.walletId } returns TEST_USER_WALLET_ID
    }

    @Test
    fun `GIVEN card signs WHEN invoke THEN signature submitted and Signed returned`() = runTest {
        // Arrange
        coEvery { authDataSource.getWithdrawalSignature(userWallet, HASH) } returns
            WithdrawalSignatureResult.Success(signature = "0xsig").right()
        coEvery { repository.submitSignature(TEST_USER_WALLET_ID, ORDER_ID, "0xsig") } returns Unit.right()

        // Act
        val actual = useCase(userWallet = userWallet, orderId = ORDER_ID, payloadHash = HASH)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpSignResult.Signed.right())
        coVerify(exactly = 1) { repository.submitSignature(TEST_USER_WALLET_ID, ORDER_ID, "0xsig") }
    }

    @Test
    fun `GIVEN user cancels NFC WHEN invoke THEN Cancelled returned and nothing submitted`() = runTest {
        // Arrange
        coEvery { authDataSource.getWithdrawalSignature(userWallet, HASH) } returns WithdrawalSignatureResult.Cancelled.right()

        // Act
        val actual = useCase(userWallet = userWallet, orderId = ORDER_ID, payloadHash = HASH)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpSignResult.Cancelled.right())
        coVerify(exactly = 0) { repository.submitSignature(any(), any(), any()) }
    }

    @Test
    fun `GIVEN signing throws WHEN invoke THEN unknown error with cause`() = runTest {
        // Arrange
        val cause = IllegalStateException("nfc")
        coEvery { authDataSource.getWithdrawalSignature(userWallet, HASH) } returns cause.left()

        // Act
        val actual = useCase(userWallet = userWallet, orderId = ORDER_ID, payloadHash = HASH)

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.Unknown(cause = cause).left())
        coVerify(exactly = 0) { repository.submitSignature(any(), any(), any()) }
    }

    private companion object {
        const val ORDER_ID = "order-1"
        const val HASH = "0xhash"
    }
}