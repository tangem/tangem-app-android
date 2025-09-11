package com.tangem.domain.account.usecase

import arrow.core.none
import arrow.core.some
import com.google.common.truth.Truth
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAccountsModeEnabledUseCaseTest {

    private val accountsCRUDRepository: AccountsCRUDRepository = mockk()
    private val featureToggles: AccountsFeatureToggles = mockk()

    private val useCase = IsAccountsModeEnabledUseCase(accountsCRUDRepository, featureToggles)

    @AfterEach
    fun tearDown() {
        clearMocks(accountsCRUDRepository, featureToggles)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Invoke {

        @Test
        fun `returns false when feature is disabled`() = runTest {
            // Arrange
            every { featureToggles.isFeatureEnabled } returns false

            // Act
            val actual = useCase.invoke().firstOrNull()

            // Assert
            Truth.assertThat(actual).isFalse()

            verify(exactly = 1) { featureToggles.isFeatureEnabled }
            verify(inverse = true) { accountsCRUDRepository.getUserWallets() }
        }

        @Test
        fun `returns false when getUserWallets emits empty flow`() = runTest {
            // Arrange
            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWallets() } returns emptyFlow()

            // Act
            val actual = useCase.invoke().firstOrNull()

            // Assert
            Truth.assertThat(actual).isFalse()

            verifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWallets()
            }

            verify(inverse = true) { accountsCRUDRepository.getTotalAccountsCount(any()) }
        }

        @Test
        fun `returns false when getUserWallets emits one wallet with isMultiCurrency false`() = runTest {
            // Arrange
            val wallet = createUserWallet(isMultiCurrency = false)

            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWallets() } returns flowOf(listOf(wallet))

            // Act
            val actual = useCase.invoke().first()

            // Assert
            Truth.assertThat(actual).isFalse()

            verifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWallets()
            }

            verify(inverse = true) { accountsCRUDRepository.getTotalAccountsCount(any()) }
        }

        @Test
        fun `returns true when getUserWallets emits one wallet with isMultiCurrency true`() = runTest {
            // Arrange
            val wallet = createUserWallet(isMultiCurrency = true)

            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWallets() } returns flowOf(listOf(wallet))
            every { accountsCRUDRepository.getTotalAccountsCount(wallet.walletId) } returns flowOf(2.some())

            // Act
            val actual = useCase.invoke().first()

            // Assert
            Truth.assertThat(actual).isTrue()

            verifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWallets()
                accountsCRUDRepository.getTotalAccountsCount(wallet.walletId)
            }
        }

        @Test
        fun `returns false when getUserWallets emits one wallet with isMultiCurrency true and None counts`() = runTest {
            // Arrange
            val wallet = createUserWallet(isMultiCurrency = true)

            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWallets() } returns flowOf(listOf(wallet))
            every { accountsCRUDRepository.getTotalAccountsCount(wallet.walletId) } returns flowOf(none())

            // Act
            val actual = useCase.invoke().first()

            // Assert
            Truth.assertThat(actual).isFalse()

            verifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWallets()
                accountsCRUDRepository.getTotalAccountsCount(wallet.walletId)
            }
        }

        @Test
        fun `returns true when getUserWallets emits two wallets, one isMultiCurrency false, one true`() = runTest {
            // Arrange
            val wallet1 = createUserWallet(isMultiCurrency = false)
            val wallet2 = createUserWallet(isMultiCurrency = true)

            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWallets() } returns flowOf(listOf(wallet1, wallet2))
            every { accountsCRUDRepository.getTotalAccountsCount(wallet2.walletId) } returns flowOf(2.some())

            // Act
            val actual = useCase.invoke().first()

            // Assert
            Truth.assertThat(actual).isTrue()

            verifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWallets()
                accountsCRUDRepository.getTotalAccountsCount(wallet2.walletId)
            }

            verify(inverse = true) { accountsCRUDRepository.getTotalAccountsCount(wallet1.walletId) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeSync {

        @Test
        fun `returns false when feature is disabled`() = runTest {
            // Arrange
            every { featureToggles.isFeatureEnabled } returns false

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isFalse()

            verify(exactly = 1) { featureToggles.isFeatureEnabled }
            verify(inverse = true) { accountsCRUDRepository.getUserWalletsSync() }
        }

        @Test
        fun `returns false when getUserWalletsSync returns empty list`() = runTest {
            // Arrange
            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWalletsSync() } returns emptyList()

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isFalse()

            verifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWalletsSync()
            }

            coVerify(inverse = true) { accountsCRUDRepository.getTotalAccountsCountSync(any()) }
        }

        @Test
        fun `returns false when getUserWalletsSync returns one wallet with isMultiCurrency false`() = runTest {
            // Arrange
            val wallet = createUserWallet(isMultiCurrency = false)

            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWalletsSync() } returns listOf(wallet)

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isFalse()

            verifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWalletsSync()
            }

            coVerify(inverse = true) { accountsCRUDRepository.getTotalAccountsCountSync(any()) }
        }

        @Test
        fun `returns true when getUserWalletsSync returns one wallet with isMultiCurrency true`() = runTest {
            // Arrange
            val wallet = createUserWallet(isMultiCurrency = true)

            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWalletsSync() } returns listOf(wallet)
            coEvery { accountsCRUDRepository.getTotalAccountsCountSync(wallet.walletId) } returns 2.some()

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isTrue()

            coVerifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWalletsSync()
                accountsCRUDRepository.getTotalAccountsCountSync(wallet.walletId)
            }
        }

        @Test
        fun `returns false when getUserWalletsSync returns multi wallet with None counts`() = runTest {
            // Arrange
            val wallet = createUserWallet(isMultiCurrency = true)

            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWalletsSync() } returns listOf(wallet)
            coEvery { accountsCRUDRepository.getTotalAccountsCountSync(wallet.walletId) } returns none()

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isFalse()

            coVerifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWalletsSync()
                accountsCRUDRepository.getTotalAccountsCountSync(wallet.walletId)
            }
        }

        @Test
        fun `returns true when getUserWalletsSync returns multi and single wallets`() = runTest {
            // Arrange
            val wallet1 = createUserWallet(isMultiCurrency = false)
            val wallet2 = createUserWallet(isMultiCurrency = true)

            every { featureToggles.isFeatureEnabled } returns true
            every { accountsCRUDRepository.getUserWalletsSync() } returns listOf(wallet1, wallet2)
            coEvery { accountsCRUDRepository.getTotalAccountsCountSync(wallet2.walletId) } returns 2.some()

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isTrue()

            coVerifyOrder {
                featureToggles.isFeatureEnabled
                accountsCRUDRepository.getUserWalletsSync()
                accountsCRUDRepository.getTotalAccountsCountSync(wallet2.walletId)
            }

            coVerify(inverse = true) { accountsCRUDRepository.getTotalAccountsCountSync(wallet1.walletId) }
        }
    }

    private fun createUserWallet(isMultiCurrency: Boolean): UserWallet = mockk {
        every { this@mockk.walletId } returns UserWalletId(stringValue = "011")
        every { this@mockk.isMultiCurrency } returns isMultiCurrency
    }
}