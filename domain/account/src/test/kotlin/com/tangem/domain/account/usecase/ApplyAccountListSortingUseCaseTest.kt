package com.tangem.domain.account.usecase

import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.usecase.ApplyAccountListSortingUseCase.Error
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplyAccountListSortingUseCaseTest {

    private val accountsCRUDRepository: AccountsCRUDRepository = mockk(relaxUnitFun = true)
    private val useCase = ApplyAccountListSortingUseCase(
        accountsCRUDRepository = accountsCRUDRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")

    @AfterEach
    fun tearDown() {
        clearMocks(accountsCRUDRepository)
    }

    @Test
    fun `invoke returns EmptyList when accountIds is empty`() = runTest {
        // Act
        val actual = useCase(accountIds = emptyList())

        // Assert
        val expected = Error.EmptyList.left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify(inverse = true) {
            accountsCRUDRepository.getAccountListSync(any())
            accountsCRUDRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke returns UnableToSortSingleAccount when accountIds contain one ID`() = runTest {
        // Act
        val actual = useCase(accountIds = listOf(mockk(relaxed = true)))

        // Assert
        val expected = Error.UnableToSortSingleAccount.left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify(inverse = true) {
            accountsCRUDRepository.getAccountListSync(any())
            accountsCRUDRepository.saveAccounts(any())
        }
    }

    @Test
    fun `invoke returns DataOperationFailed when getAccountList returns error`() = runTest {
        // Arrange
        val accountList = createAccountList()
        val accountIds = accountList.toAccountIds()

        val exception = Exception("Test error")
        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } throws exception

        // Act
        val actual = useCase(accountIds)

        // Assert
        val expected = Error.DataOperationFailed(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { accountsCRUDRepository.getAccountListSync(userWalletId) }
        coVerify(inverse = true) { accountsCRUDRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke returns UnableToSortSingleAccount when saved accountList contain one account`() = runTest {
        // Arrange
        val accountIds = createAccountList().toAccountIds()

        coEvery {
            accountsCRUDRepository.getAccountListSync(userWalletId)
        } returns AccountList.empty(userWalletId).some()

        // Act
        val actual = useCase(accountIds)

        // Assert
        val expected = Error.UnableToSortSingleAccount.left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { accountsCRUDRepository.getAccountListSync(userWalletId) }
        coVerify(inverse = true) { accountsCRUDRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke returns SomeAccountsNotFound when saved AccountList doesn't contain unknown ID`() = runTest {
        // Arrange
        val unknownAccountId = AccountId.forMainCryptoPortfolio(
            userWalletId = UserWalletId("012"),
        )

        val accountList = createAccountList()
        val accountIds = listOf(accountList.mainAccount.accountId, unknownAccountId)

        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        // Act
        val actual = useCase(accountIds)

        // Assert
        val expected = Error.SomeAccountsNotFound.left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { accountsCRUDRepository.getAccountListSync(userWalletId) }
        coVerify(inverse = true) { accountsCRUDRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke returns Right without accounts saving when nothing to change`() = runTest {
        // Arrange
        val accountList = createAccountList()
        val accountIds = accountList.toAccountIds()

        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        // Act
        val actual = useCase(accountIds)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { accountsCRUDRepository.getAccountListSync(userWalletId) }
        coVerify(inverse = true) { accountsCRUDRepository.saveAccounts(any()) }
    }

    @Test
    fun `invoke returns Right`() = runTest {
        // Arrange
        val accountList = createAccountList()
        val accountIds = accountList.toAccountIds().reversed()

        val updatedAccountList = AccountList(
            userWalletId = accountList.userWalletId,
            accounts = accountList.accounts.reversed(),
            totalAccounts = accountList.totalAccounts,
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        ).getOrNull()!!

        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        // Act
        val actual = useCase(accountIds)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            accountsCRUDRepository.getAccountListSync(userWalletId)
            accountsCRUDRepository.saveAccounts(updatedAccountList)
        }
    }

    private fun createAccountList(): AccountList {
        val accountList = AccountList.empty(userWalletId)

        val account1 = Account.CryptoPortfolio(
            accountId = AccountId.forCryptoPortfolio(userWalletId, DerivationIndex(1).getOrNull()!!),
            name = "Account 1",
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = 1,
        )
            .getOrNull()!!

        return (accountList + account1).getOrNull()!!
    }

    private fun AccountList.toAccountIds(): List<AccountId> {
        return this.accounts.map { it.accountId }
    }
}