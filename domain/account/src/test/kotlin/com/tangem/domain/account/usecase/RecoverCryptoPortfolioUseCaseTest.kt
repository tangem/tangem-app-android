package com.tangem.domain.account.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.usecase.RecoverCryptoPortfolioUseCase.Error
import com.tangem.domain.account.utils.createAccount
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecoverCryptoPortfolioUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = RecoverCryptoPortfolioUseCase(crudRepository)
    private val userWallet = mockk<UserWallet>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository, userWallet)
        every { userWallet.walletId } returns userWalletId
    }

    @Test
    fun `invoke should recover archived crypto portfolio account`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = AccountList.empty(userWallet)
        val archivedAccount = ArchivedAccount(
            accountId = account.accountId,
            name = account.accountName,
            icon = account.icon,
            derivationIndex = account.derivationIndex,
            tokensCount = 1,
            networksCount = 1,
        )

        val updatedAccountList = (accountList + account).getOrNull()!!

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = account.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    @Test
    fun `invoke should return error if getAccounts returns None`() = runTest {
        // Arrange
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Main,
        )

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns None

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Error.CriticalTechError.AccountsNotCreated(userWalletId).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccountListSync(userWalletId) }
        coVerify(inverse = true) {
            crudRepository.getArchivedAccountSync(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if getAccounts throws exception`() = runTest {
        // Arrange
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Main,
        )
        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccountListSync(userWalletId) } throws exception

        // Act
        val actual = useCase(accountId)

        // Assert
        val expected = Error.DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccountListSync(userWalletId) }
        coVerify(inverse = true) {
            crudRepository.getArchivedAccountSync(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if getArchivedAccount throws exception`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = AccountList.empty(userWallet)
        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } throws exception

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = Error.DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
        }
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if getArchivedAccount returns null`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = AccountList.empty(userWallet)

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns None

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = Error.CriticalTechError.AccountNotFound(account.accountId).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
        }
        coVerify(inverse = true) { crudRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke should return error if saveAccounts throws exception`() = runTest {
        // Arrange
        val account = createAccount(userWalletId)
        val accountList = AccountList.empty(userWallet)
        val archivedAccount = ArchivedAccount(
            accountId = account.accountId,
            name = account.accountName,
            icon = account.icon,
            derivationIndex = account.derivationIndex,
            tokensCount = 1,
            networksCount = 1,
        )

        val updatedAccountList = (accountList + account).getOrNull()!!
        val exception = IllegalStateException("Save failed")

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.getArchivedAccountSync(account.accountId) } returns archivedAccount.toOption()
        coEvery { crudRepository.saveAccounts(updatedAccountList) } throws exception

        // Act
        val actual = useCase(account.accountId)

        // Assert
        val expected = Error.DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getArchivedAccountSync(account.accountId)
            crudRepository.saveAccounts(updatedAccountList)
        }
    }

    private companion object {
        val userWalletId = UserWalletId("011")
    }
}