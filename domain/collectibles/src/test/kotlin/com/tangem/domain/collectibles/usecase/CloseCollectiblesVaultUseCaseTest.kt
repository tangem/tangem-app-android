package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CloseCollectiblesVaultUseCaseTest {

    private val gachaVaultRepository: GachaVaultRepository = mockk()
    private val accountRepository: CollectiblesAccountRepository = mockk(relaxed = true)

    private val useCase = CloseCollectiblesVaultUseCase(
        gachaVaultRepository = gachaVaultRepository,
        accountRepository = accountRepository,
    )

    private val userWallet: UserWallet = mockk {
        every { walletId } returns WALLET_ID
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(gachaVaultRepository, accountRepository)
    }

    @Test
    fun `GIVEN the chain closes the vault WHEN invoked THEN the account record is cleared`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.closeVault(userWallet) } returns Unit.right()

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { accountRepository.clearAccount(WALLET_ID) }
    }

    @Test
    fun `GIVEN the card prompt is dismissed WHEN invoked THEN the account record is kept`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.closeVault(userWallet) } returns CollectiblesAccountError.CardError(code = 50002, message = "50002").left()

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountError.CardError(code = 50002, message = "50002").left())
        coVerify(exactly = 0) { accountRepository.clearAccount(any()) }
    }

    private companion object {

        val WALLET_ID = UserWalletId("011977")
    }
}