package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetPolymarketEligibleWalletsUseCaseTest {

    private val repository: UserWalletsListRepository = mockk()

    private val useCase = GetPolymarketEligibleWalletsUseCase(repository)

    private val unlocked = wallet(id = UserWalletId("011"), locked = false)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
        coEvery { repository.load() } just Runs
    }

    @Test
    fun `GIVEN locked and unlocked wallets WHEN invoked THEN only unlocked are returned`() = runTest {
        // Arrange
        val locked = wallet(id = UserWalletId("022"), locked = true)
        every { repository.userWallets } returns MutableStateFlow(listOf(unlocked, locked))

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).containsExactly(unlocked)
    }

    @Test
    fun `GIVEN a single-currency wallet WHEN invoked THEN it is not eligible`() = runTest {
        // Arrange
        val singleCurrency = wallet(id = UserWalletId("033"), locked = false, multiCurrency = false)
        every { repository.userWallets } returns MutableStateFlow(listOf(unlocked, singleCurrency))

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).containsExactly(unlocked)
    }

    @Test
    fun `GIVEN every wallet is single-currency WHEN invoked THEN returns empty`() = runTest {
        // Arrange
        val singleCurrency = wallet(id = UserWalletId("033"), locked = false, multiCurrency = false)
        every { repository.userWallets } returns MutableStateFlow(listOf(singleCurrency))

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN the list is not loaded WHEN invoked THEN returns empty`() = runTest {
        // Arrange
        every { repository.userWallets } returns MutableStateFlow(null)

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN wallets are not loaded yet WHEN invoked THEN they are loaded first`() = runTest {
        // Arrange
        val wallets = MutableStateFlow<List<UserWallet>?>(null)
        every { repository.userWallets } returns wallets
        coEvery { repository.load() } coAnswers { wallets.value = listOf(unlocked) }

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).containsExactly(unlocked)
        coVerify(exactly = 1) { repository.load() }
    }

    private fun wallet(id: UserWalletId, locked: Boolean, multiCurrency: Boolean = true): UserWallet =
        mockk<UserWallet.Cold> {
            every { walletId } returns id
            every { isLocked } returns locked
            every { isMultiCurrency } returns multiCurrency
        }
}