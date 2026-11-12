package com.tangem.features.polymarket.impl.common.walletlink

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.EnsureWalletLinkedUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PolymarketWalletLinkGateTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val ensureWalletLinkedUseCase: EnsureWalletLinkedUseCase = mockk()
    private val userWallet: UserWallet = mockk()

    private val gate = PolymarketWalletLinkGate(
        getUserWalletUseCase = getUserWalletUseCase,
        ensureWalletLinkedUseCase = ensureWalletLinkedUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(getUserWalletUseCase, ensureWalletLinkedUseCase)
        every { getUserWalletUseCase(WALLET_ID) } returns userWallet.right()
    }

    @Test
    fun `GIVEN the wallet is linked WHEN ensureLinked THEN returns true`() = runTest {
        // Arrange
        coEvery { ensureWalletLinkedUseCase(userWallet) } returns true

        // Act
        val actual = gate.ensureLinked(WALLET_ID)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN the user does not link the wallet WHEN ensureLinked THEN returns false`() = runTest {
        // Arrange
        coEvery { ensureWalletLinkedUseCase(userWallet) } returns false

        // Act
        val actual = gate.ensureLinked(WALLET_ID)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN the wallet is gone WHEN ensureLinked THEN returns false without asking`() = runTest {
        // Arrange
        every { getUserWalletUseCase(WALLET_ID) } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        val actual = gate.ensureLinked(WALLET_ID)

        // Assert
        assertThat(actual).isFalse()
        coVerify(exactly = 0) { ensureWalletLinkedUseCase(any()) }
    }

    @Test
    fun `GIVEN the link sheet never appears WHEN ensureLinked THEN returns false instead of cancelling`() = runTest {
        // Arrange
        coEvery { ensureWalletLinkedUseCase(userWallet) } coAnswers {
            withTimeout(timeMillis = 1_000) { awaitCancellation() }
        }

        // Act
        val actual = gate.ensureLinked(WALLET_ID)

        // Assert
        assertThat(actual).isFalse()
    }

    private companion object {
        val WALLET_ID = UserWalletId("aa")
    }
}