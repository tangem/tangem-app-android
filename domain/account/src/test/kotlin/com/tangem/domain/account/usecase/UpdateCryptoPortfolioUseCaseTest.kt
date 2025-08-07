package com.tangem.domain.account.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.usecase.UpdateCryptoPortfolioUseCase.Error
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
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
class UpdateCryptoPortfolioUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = UpdateCryptoPortfolioUseCase(crudRepository = crudRepository)

    private val userWallet = mockk<UserWallet>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository, userWallet)

        every { userWallet.walletId } returns userWalletId
    }

    @Test
    fun `invoke should update crypto portfolio account with new name`() = runTest {
        // Arrange
        val accountList = AccountList.createEmpty(userWallet = userWallet)
        val accountId = accountList.mainAccount.accountId

        val newAccountName = AccountName("New name").getOrNull()!!
        val updatedAccount = accountList.mainAccount.copy(accountName = newAccountName)
        val updatedAccountList = (accountList + updatedAccount).getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId = accountId, accountName = newAccountName)

        // Assert
        val expected = updatedAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId = userWalletId)
            crudRepository.saveAccounts(accountList = updatedAccountList)
        }
    }

    @Test
    fun `invoke should update crypto portfolio account with new icon`() = runTest {
        // Arrange
        val accountList = AccountList.createEmpty(userWallet = userWallet)
        val accountId = accountList.mainAccount.accountId

        val newAccountIcon = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Star,
            color = CryptoPortfolioIcon.Color.CaribbeanBlue,
        )
        val updatedAccount = accountList.mainAccount.copy(accountIcon = newAccountIcon)
        val updatedAccountList = (accountList + updatedAccount).getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId = accountId, icon = newAccountIcon)

        // Assert
        val expected = updatedAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId = userWalletId)
            crudRepository.saveAccounts(accountList = updatedAccountList)
        }
    }

    @Test
    fun `invoke should update crypto portfolio account with new name and icon`() = runTest {
        // Arrange
        val accountList = AccountList.createEmpty(userWallet = userWallet)
        val accountId = accountList.mainAccount.accountId

        val newAccountName = AccountName("New name").getOrNull()!!
        val newAccountIcon = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Star,
            color = CryptoPortfolioIcon.Color.CaribbeanBlue,
        )
        val updatedAccount = accountList.mainAccount.copy(accountName = newAccountName, accountIcon = newAccountIcon)
        val updatedAccountList = (accountList + updatedAccount).getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId = accountId, accountName = newAccountName, icon = newAccountIcon)

        // Assert
        val expected = updatedAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId = userWalletId)
            crudRepository.saveAccounts(accountList = updatedAccountList)
        }
    }

    @Test
    fun `invoke if name and icon are null`() = runTest {
        // Arrange
        val accountList = AccountList.createEmpty(userWallet = userWallet)
        val accountId = accountList.mainAccount.accountId

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId = accountId)

        // Assert
        val expected = Error.NothingToUpdate.left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify(inverse = true) {
            crudRepository.getAccounts(userWalletId = any())
            crudRepository.saveAccounts(accountList = any())
        }
    }

    @Test
    fun `invoke if getAccounts throws exception`() = runTest {
        // Arrange
        val accountList = AccountList.createEmpty(userWallet = userWallet)
        val accountId = accountList.mainAccount.accountId

        val newAccountName = AccountName("New name").getOrNull()!!

        val exception = IllegalStateException("Test exception")

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } throws exception

        // Act
        val actual = useCase(accountId = accountId, accountName = newAccountName)

        // Assert
        val expected = Error.DataOperationFailed(cause = exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccounts(userWalletId = userWalletId) }
        coVerify(inverse = true) { crudRepository.saveAccounts(accountList = any()) }
    }

    @Test
    fun `invoke if getAccounts returns None`() = runTest {
        // Arrange
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex.Main,
        )
        val accountList = None

        val newAccountName = AccountName("New name").getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList

        // Act
        val actual = useCase(accountId = accountId, accountName = newAccountName)

        // Assert
        val expected = Error.CriticalTechError.AccountsNotCreated(userWalletId = userWalletId).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccounts(userWalletId = userWalletId) }
        coVerify(inverse = true) { crudRepository.saveAccounts(accountList = any()) }
    }

    @Test
    fun `invoke if getAccounts does not contain accountId`() = runTest {
        // Arrange
        val accountList = AccountList.createEmpty(userWallet = userWallet)
        val accountId = AccountId.forCryptoPortfolio(
            userWalletId = userWalletId,
            derivationIndex = DerivationIndex(1).getOrNull()!!,
        )

        val newAccountName = AccountName("New name").getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId = accountId, accountName = newAccountName)

        // Assert
        val expected = Error.CriticalTechError.AccountNotFound(accountId = accountId).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccounts(userWalletId = userWalletId) }
        coVerify(inverse = true) { crudRepository.saveAccounts(accountList = any()) }
    }

    @Test
    fun `invoke if saveAccounts throws exception`() = runTest {
        // Arrange
        val accountList = AccountList.createEmpty(userWallet = userWallet)
        val accountId = accountList.mainAccount.accountId

        val newAccountName = AccountName("New name").getOrNull()!!
        val updatedAccount = accountList.mainAccount.copy(accountName = newAccountName)
        val updatedAccountList = (accountList + updatedAccount).getOrNull()!!

        val exception = IllegalStateException("Save failed")

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.saveAccounts(accountList = updatedAccountList) } throws exception

        // Act
        val actual = useCase(accountId = accountId, accountName = newAccountName)

        // Assert
        val expected = Error.DataOperationFailed(cause = exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccounts(userWalletId = userWalletId)
            crudRepository.saveAccounts(accountList = updatedAccountList)
        }
    }

    private companion object {

        val userWalletId = UserWalletId("011")
    }
}