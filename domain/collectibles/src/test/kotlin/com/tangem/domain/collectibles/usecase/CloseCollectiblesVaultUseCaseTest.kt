package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.delegate.CollectiblesSessionKey
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CloseCollectiblesVaultUseCaseTest {

    private val gachaVaultRepository: GachaVaultRepository = mockk()
    private val accountRepository: CollectiblesAccountRepository = mockk(relaxed = true)
    private val sessionKey: CollectiblesSessionKey = mockk(relaxed = true)

    private val useCase = CloseCollectiblesVaultUseCase(
        gachaVaultRepository = gachaVaultRepository,
        accountRepository = accountRepository,
        sessionKey = sessionKey,
    )

    private val userWallet: UserWallet = mockk {
        every { walletId } returns WALLET_ID
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(gachaVaultRepository, accountRepository, sessionKey)
    }

    @Test
    fun `GIVEN the chain closes the vault WHEN invoked THEN the session key, the record and the backend row go`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.closeVault(userWallet) } returns Unit.right()

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            sessionKey.forget(AccountId.forCollectiblesAccount(userWalletId = WALLET_ID))
            accountRepository.clearAccount(WALLET_ID)
            accountRepository.unregisterAccount(WALLET_ID)
        }
    }

    @Test
    fun `GIVEN the card prompt is dismissed WHEN invoked THEN the account record is kept`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.closeVault(userWallet) } returns CollectiblesAccountError.CardError(code = 50002, message = "50002").left()

        // Act
        val actual = useCase(userWallet)

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountError.CardError(code = 50002, message = "50002").left())
        coVerify(exactly = 0) {
            sessionKey.forget(any())
            accountRepository.clearAccount(any())
            accountRepository.unregisterAccount(any())
        }
    }

    private companion object {

        val WALLET_ID = UserWalletId("011977")
    }
}