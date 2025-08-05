package com.tangem.domain.account.usecase

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.usecase.UpdateCryptoPortfolioUseCase.Error
import com.tangem.domain.account.utils.randomAccountId
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateCryptoPortfolioUseCaseTest {

    private val crudRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = UpdateCryptoPortfolioUseCase(crudRepository = crudRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(crudRepository)
    }

    @Test
    fun `invoke should update crypto portfolio account with new name`() = runTest {
        // Arrange
        val accountId = AccountId(value = "test-account-id", userWalletId = mockk())
        val userWalletId = accountId.userWalletId
        val account = createAccount(accountId = accountId, isMain = true)
        val accountList = AccountList(
            userWallet = mockk(),
            accounts = setOf(account),
            totalAccounts = 1,
        )
            .getOrNull()!!

        val newAccountName = AccountName("New name").getOrNull()!!

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId = accountId, accountName = newAccountName)

        // Assert
        val updatedAccount = account.copy(accountName = newAccountName)
        val expected = updatedAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        val updatedAccountList = (accountList + updatedAccount).getOrNull()!!
        coVerifyOrder {
            crudRepository.getAccounts(userWalletId = userWalletId)
            crudRepository.saveAccounts(accountList = updatedAccountList)
        }
    }

    @Test
    fun `invoke should update crypto portfolio account with new icon`() = runTest {
        // Arrange
        val accountId = AccountId(value = "test-account-id", userWalletId = mockk())
        val userWalletId = accountId.userWalletId
        val account = createAccount(accountId = accountId, isMain = true)
        val accountList = AccountList(
            userWallet = mockk(),
            accounts = setOf(account),
            totalAccounts = 1,
        )
            .getOrNull()!!

        val newAccountIcon = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Star,
            color = CryptoPortfolioIcon.Color.CaribbeanBlue,
        )

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId = accountId, icon = newAccountIcon)

        // Assert
        val updatedAccount = account.copy(accountIcon = newAccountIcon)
        val expected = updatedAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        val updatedAccountList = (accountList + updatedAccount).getOrNull()!!
        coVerifyOrder {
            crudRepository.getAccounts(userWalletId = userWalletId)
            crudRepository.saveAccounts(accountList = updatedAccountList)
        }
    }

    @Test
    fun `invoke should update crypto portfolio account with new name and icon`() = runTest {
        // Arrange
        val accountId = AccountId(value = "test-account-id", userWalletId = mockk())
        val userWalletId = accountId.userWalletId
        val account = createAccount(accountId = accountId, isMain = true)
        val accountList = AccountList(
            userWallet = mockk(),
            accounts = setOf(account),
            totalAccounts = 1,
        )
            .getOrNull()!!

        val newAccountName = AccountName("New name").getOrNull()!!
        val newAccountIcon = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Star,
            color = CryptoPortfolioIcon.Color.CaribbeanBlue,
        )

        coEvery { crudRepository.getAccounts(userWalletId = userWalletId) } returns accountList.toOption()

        // Act
        val actual = useCase(accountId = accountId, accountName = newAccountName, icon = newAccountIcon)

        // Assert
        val updatedAccount = account.copy(accountName = newAccountName, accountIcon = newAccountIcon)
        val expected = updatedAccount.right()
        Truth.assertThat(actual).isEqualTo(expected)

        val updatedAccountList = (accountList + updatedAccount).getOrNull()!!
        coVerifyOrder {
            crudRepository.getAccounts(userWalletId = userWalletId)
            crudRepository.saveAccounts(accountList = updatedAccountList)
        }
    }

    @Test
    fun `invoke if name and icon are null`() = runTest {
        // Arrange
        val accountId = AccountId(value = "test-account-id", userWalletId = mockk())
        val userWalletId = accountId.userWalletId
        val account = createAccount(accountId = accountId, isMain = true)
        val accountList = AccountList(
            userWallet = mockk(),
            accounts = setOf(account),
            totalAccounts = 1,
        )
            .getOrNull()!!

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
        val accountId = AccountId(value = "test-account-id", userWalletId = mockk())
        val userWalletId = accountId.userWalletId

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
        val accountId = AccountId(value = "test-account-id", userWalletId = mockk())
        val userWalletId = accountId.userWalletId
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
        val accountId = AccountId(value = "test-account-id", userWalletId = mockk())
        val userWalletId = accountId.userWalletId
        val account = createAccount(
            accountId = AccountId(value = "another-account-id", userWalletId = mockk()),
            isMain = true,
        )
        val accountList = AccountList(
            userWallet = mockk(),
            accounts = setOf(account),
            totalAccounts = 1,
        )
            .getOrNull()!!

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
        val accountId = AccountId(value = "test-account-id", userWalletId = mockk())
        val userWalletId = accountId.userWalletId
        val account = createAccount(accountId = accountId, isMain = true)
        val accountList = AccountList(
            userWallet = mockk(),
            accounts = setOf(account),
            totalAccounts = 1,
        ).getOrNull()!!

        val newAccountName = AccountName("New name").getOrNull()!!
        val updatedAccount = account.copy(accountName = newAccountName)
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

    private fun createAccount(
        accountId: AccountId = AccountId(value = randomAccountId(length = 5), userWalletId = mockk()),
        accountIcon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        isMain: Boolean,
    ): Account.CryptoPortfolio {
        return Account.CryptoPortfolio(
            accountId = accountId,
            name = "Test Account",
            accountIcon = accountIcon,
            derivationIndex = if (isMain) 0 else Random.nextInt(1, 21),
            isArchived = false,
            cryptoCurrencyList = Account.CryptoPortfolio.CryptoCurrencyList(
                currencies = emptySet(),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            ),
        )
            .getOrNull()!!
    }
}