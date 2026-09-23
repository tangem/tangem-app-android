package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpSignResult
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SignBankOfframpOrderUseCaseTest {

    private val repository: BankOfframpOrderRepository = mockk()
    private val authDataSource: TangemPayAuthDataSource = mockk()
    private val userWallet: UserWallet = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val wallets = MutableStateFlow<List<UserWallet>?>(null)

    private val useCase = SignBankOfframpOrderUseCase(
        repository = repository,
        authDataSource = authDataSource,
        userWalletsListRepository = userWalletsListRepository,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, authDataSource, userWallet, userWalletsListRepository)
        every { userWallet.walletId } returns TEST_USER_WALLET_ID
        wallets.value = listOf(userWallet)
        every { userWalletsListRepository.userWallets } returns wallets
    }

    @Test
    fun `GIVEN card signs WHEN invoke THEN signature submitted and Signed returned`() = runTest {
        // Arrange
        coEvery { authDataSource.getWithdrawalSignature(userWallet, HASH) } returns
            WithdrawalSignatureResult.Success(signature = "0xsig").right()
        coEvery { repository.submitSignature(TEST_USER_WALLET_ID, ORDER_ID, "0xsig", KEY) } returns Unit.right()

        // Act
        val actual = sign()

        // Assert
        assertThat(actual).isEqualTo(BankOfframpSignResult.Signed.right())
        coVerify(exactly = 1) { repository.submitSignature(TEST_USER_WALLET_ID, ORDER_ID, "0xsig", KEY) }
    }

    @Test
    fun `GIVEN user cancels NFC WHEN invoke THEN Cancelled returned and nothing submitted`() = runTest {
        // Arrange
        coEvery { authDataSource.getWithdrawalSignature(userWallet, HASH) } returns WithdrawalSignatureResult.Cancelled.right()

        // Act
        val actual = sign()

        // Assert
        assertThat(actual).isEqualTo(BankOfframpSignResult.Cancelled.right())
        coVerify(exactly = 0) { repository.submitSignature(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN signing throws WHEN invoke THEN unknown error with cause`() = runTest {
        // Arrange
        val cause = IllegalStateException("nfc")
        coEvery { authDataSource.getWithdrawalSignature(userWallet, HASH) } returns cause.left()

        // Act
        val actual = sign()

        // Assert
        assertThat(actual).isEqualTo(BankOfframpError.Unknown(cause = cause).left())
        coVerify(exactly = 0) { repository.submitSignature(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the wallet is not in the list WHEN invoke THEN unknown error and nothing signed`() = runTest {
        // Arrange
        wallets.value = emptyList()

        // Act
        val actual = sign()

        // Assert
        assertThat(actual.isLeft()).isTrue()
        coVerify(exactly = 0) { authDataSource.getWithdrawalSignature(any(), any()) }
        coVerify(exactly = 0) { repository.submitSignature(any(), any(), any(), any()) }
    }

    private suspend fun sign() = useCase(
        userWalletId = TEST_USER_WALLET_ID,
        orderId = ORDER_ID,
        payloadHash = HASH,
        idempotencyKey = KEY,
    )

    private companion object {
        const val ORDER_ID = "order-1"
        const val HASH = "0xhash"
        const val KEY = "idempotency-key"
    }
}