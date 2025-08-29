package com.tangem.domain.account.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.utils.createAccount
import com.tangem.domain.account.utils.createAccounts
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddCryptoPortfolioUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = AddCryptoPortfolioUseCase(crudRepository)

    private val userWallet = mockk<UserWallet>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository, userWallet)

        every { userWallet.walletId } returns userWalletId
    }

    @Test
    fun `invoke should add new crypto portfolio account to existing list`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val accountList = AccountList.empty(userWallet)
        val updatedAccountList = (accountList + newAccount).getOrNull()!!

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = newAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
        }

        coVerify(inverse = true) { crudRepository.getUserWallet(userWalletId) }
    }

    @Test
    fun `invoke should create new account list if none exists`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val newAccountList = (AccountList.empty(userWallet) + newAccount).getOrNull()!!

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns None
        coEvery { crudRepository.getUserWallet(userWalletId) } returns userWallet

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = newAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.getUserWallet(userWalletId)
            crudRepository.saveAccounts(newAccountList)
        }
    }

    @Test
    fun `invoke should return error if account list requirements not met`() = runTest {
        // Arrange
        val accountList = AccountList(
            userWallet = userWallet,
            accounts = createAccounts(userWalletId = userWalletId, count = 20),
            totalAccounts = 20,
        ).getOrNull()!!

        val newAccount = createNewAccount(derivationIndex = 21)

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = AddCryptoPortfolioUseCase.Error.AccountListRequirementsNotMet(
            cause = AccountList.Error.ExceedsMaxAccountsCount,
        ).left()

        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccountListSync(userWalletId) }

        coVerify(inverse = true) {
            crudRepository.getUserWallet(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if getAccounts throws exception`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccountListSync(userWalletId) } throws exception

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = AddCryptoPortfolioUseCase.Error.DataOperationFailed(cause = exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { crudRepository.getAccountListSync(userWalletId) }

        coVerify(inverse = true) {
            crudRepository.getUserWallet(any())
            crudRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke should return error if saveAccounts throws exception`() = runTest {
        // Arrange
        val newAccount = createNewAccount()
        val accountList = AccountList.empty(userWallet)
        val updatedAccountList = (accountList + newAccount).getOrNull()!!

        val exception = IllegalStateException("Test error")

        coEvery { crudRepository.getAccountListSync(userWalletId) } returns accountList.toOption()
        coEvery { crudRepository.saveAccounts(updatedAccountList) } throws exception

        // Act
        val actual = useCase(
            userWalletId = userWalletId,
            accountName = newAccount.accountName,
            icon = newAccount.icon,
            derivationIndex = newAccount.derivationIndex,
        )

        // Assert
        val expected = AddCryptoPortfolioUseCase.Error.DataOperationFailed(cause = exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            crudRepository.getAccountListSync(userWalletId)
            crudRepository.saveAccounts(updatedAccountList)
        }

        coVerify(inverse = true) { crudRepository.getUserWallet(userWalletId) }
    }

    private companion object {

        val userWalletId = UserWalletId("011")

        fun createNewAccount(derivationIndex: Int = 1): Account.CryptoPortfolio {
            return createAccount(
                userWalletId = userWalletId,
                name = "New Account",
                icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
                derivationIndex = derivationIndex,
            )
        }
    }
}