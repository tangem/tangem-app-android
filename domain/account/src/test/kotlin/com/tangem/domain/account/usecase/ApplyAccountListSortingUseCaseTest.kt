package com.tangem.domain.account.usecase

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.usecase.ApplyAccountListSortingUseCase.Error
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

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
        val accountIds = defaultAccountList.toAccountIds()

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
        val accountIds = defaultAccountList.toAccountIds()

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
    fun `invoke returns Right without accounts saving when nothing to change`() = runTest {
        // Arrange
        val accountList = defaultAccountList
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
        val accountList = defaultAccountList
        val accountIds = accountList.toAccountIds().reversed()

        val updatedAccountList = AccountList(
            userWalletId = accountList.userWalletId,
            accounts = accountList.accounts.reversed(),
            totalAccounts = accountList.totalAccounts,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
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

    @Test
    fun `GIVEN list with an active joint account WHEN invoke THEN reordered list keeps the joint counter`() = runTest {
        // Arrange
        val accountList = MockAccounts.createAccountList(activeAccounts = 2, jointAccounts = 1)
        val accountIds = accountList.toAccountIds().reversed()

        val updatedAccountList = AccountList(
            userWalletId = accountList.userWalletId,
            accounts = accountList.accounts.reversed(),
            totalAccounts = accountList.totalAccounts,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalJointAccounts = accountList.totalJointAccounts,
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

    @Test
    fun `GIVEN no saved account list WHEN invoke THEN DataOperationFailed and nothing is saved`() = runTest {
        // Arrange
        val accountIds = defaultAccountList.toAccountIds()
        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns none()

        // Act
        val actual = useCase(accountIds)

        // Assert
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(Error.DataOperationFailed::class.java)
        coVerify(inverse = true) { accountsCRUDRepository.saveAccounts(any()) }
    }

    @Test
    fun `GIVEN the remote save fails WHEN invoke THEN the order is saved locally`() = runTest {
        // Arrange
        val accountList = defaultAccountList
        val accountIds = accountList.toAccountIds().reversed()
        val updatedAccountList = accountList.withAccounts(accounts = accountList.accounts.reversed()).getOrNull()!!

        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()
        coEvery { accountsCRUDRepository.saveAccounts(updatedAccountList) } throws IllegalStateException("No network")

        // Act
        val actual = useCase(accountIds)

        // Assert
        Truth.assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            accountsCRUDRepository.saveAccounts(updatedAccountList)
            accountsCRUDRepository.saveAccountsLocally(updatedAccountList)
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN ids from the settings WHEN invoke THEN the saved order follows them`(model: SortModel) = runTest {
        // Arrange
        val accountList = fourAccountList.withAccounts(accounts = model.saved).getOrNull()!!
        val updatedAccountList = accountList.withAccounts(accounts = model.expected).getOrNull()!!

        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        // Act
        val actual = useCase(model.ids)

        // Assert
        Truth.assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { accountsCRUDRepository.saveAccounts(updatedAccountList) }
    }

    data class SortModel(
        val description: String,
        val saved: List<Account>,
        val ids: List<AccountId>,
        val expected: List<Account>,
    ) {
        override fun toString(): String = description
    }

    private fun provideTestModels(): List<SortModel> {
        val (a, b, c, d) = fourAccountList.accounts
        val collectibles = Account.Collectibles(userWalletId = fourAccountList.userWalletId)
        val unknownId = AccountId.forMainCryptoPortfolio(userWalletId = UserWalletId("012"))

        return listOf(
            SortModel(
                description = "collectibles listed: moved like a portfolio",
                saved = listOf(collectibles, a, b),
                ids = listOf(a, collectibles, b).map { it.accountId },
                expected = listOf(a, collectibles, b),
            ),
            SortModel(
                description = "collectibles hidden: keeps its index",
                saved = listOf(a, collectibles, b),
                ids = listOf(b, a).map { it.accountId },
                expected = listOf(b, collectibles, a),
            ),
            SortModel(
                description = "collectibles hidden first: stays first",
                saved = listOf(collectibles, a, b),
                ids = listOf(b, a).map { it.accountId },
                expected = listOf(collectibles, b, a),
            ),
            SortModel(
                description = "portfolio not listed: keeps its index",
                saved = listOf(a, b, c),
                ids = listOf(c, a).map { it.accountId },
                expected = listOf(c, b, a),
            ),
            SortModel(
                description = "several not listed: each keeps its index",
                saved = listOf(a, b, c, d),
                ids = listOf(d, b).map { it.accountId },
                expected = listOf(a, d, c, b),
            ),
            SortModel(
                description = "unknown id: ignored",
                saved = listOf(a, b),
                ids = listOf(b.accountId, unknownId, a.accountId),
                expected = listOf(b, a),
            ),
        )
    }

    private fun AccountList.toAccountIds(): List<AccountId> {
        return this.accounts.map { it.accountId }
    }

    private companion object {

        val defaultAccountList = MockAccounts.createAccountList(activeAccounts = 2)
        val fourAccountList = MockAccounts.createAccountList(activeAccounts = 4)
    }
}