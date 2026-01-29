package com.tangem.domain.account.status.usecase

import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplyTokenListSortingUseCaseTest {

    private val accountsCRUDRepository = mockk<AccountsCRUDRepository>(relaxUnitFun = true)

    private val useCase = ApplyTokenListSortingUseCaseV2(
        accountsCRUDRepository = accountsCRUDRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId(stringValue = "011")

    @AfterEach
    fun tearDown() {
        clearMocks(accountsCRUDRepository)
    }

    @Test
    fun `when tokens are empty then error should be received`() = runTest {
        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = emptyMap(),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        // Assert
        val expected = TokenListSortingError.TokenListIsEmpty.left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder(inverse = true) {
            accountsCRUDRepository.getAccountListSync(userWalletId = any())
            accountsCRUDRepository.saveAccountsLocally(accountList = any())
            accountsCRUDRepository.syncTokens(userWalletId = any())
        }
    }

    @Test
    fun `when getAccountListSync throws exception then error should be received`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId = userWalletId, cryptoCurrencies = emptySet())

        val exception = IllegalStateException("No internet connection")
        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } throws exception

        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = mapOf(accountList.mainAccount to emptyList()),
            isGroupedByNetwork = true,
            isSortedByBalance = false,
        )

        // Assert
        val expected = TokenListSortingError.DataError(exception).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder { accountsCRUDRepository.getAccountListSync(userWalletId) }

        coVerifyOrder(inverse = true) {
            accountsCRUDRepository.saveAccountsLocally(accountList = any())
            accountsCRUDRepository.syncTokens(userWalletId = any())
        }
    }

    @Test
    fun `when saveAccountsLocally throws exception then error should be received`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId = userWalletId, cryptoCurrencies = emptySet())

        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        val exception = IllegalStateException("No internet connection")
        coEvery { accountsCRUDRepository.saveAccountsLocally(accountList) } throws exception

        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = mapOf(accountList.mainAccount to emptyList()),
            isGroupedByNetwork = true,
            isSortedByBalance = false,
        ).leftOrNull() as TokenListSortingError.DataError

        // Assert
        val expected = TokenListSortingError.DataError(IllegalStateException(exception.toString()))
        Truth.assertThat(actual.cause).isInstanceOf(expected.cause::class.java)
        Truth.assertThat(actual.cause).hasMessageThat().isEqualTo(expected.cause.message)

        coVerifyOrder {
            accountsCRUDRepository.getAccountListSync(userWalletId)
            accountsCRUDRepository.saveAccountsLocally(accountList)
            accountsCRUDRepository.syncTokens(userWalletId = userWalletId)
        }
    }

    @Test
    fun `when syncTokens throws exception then error should be received`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId = userWalletId, cryptoCurrencies = emptySet())

        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        val exception = IllegalStateException("No internet connection")
        coEvery { accountsCRUDRepository.syncTokens(userWalletId) } throws exception

        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = mapOf(accountList.mainAccount to emptyList()),
            isGroupedByNetwork = true,
            isSortedByBalance = false,
        ).leftOrNull() as TokenListSortingError.DataError

        // Assert
        val expected = TokenListSortingError.DataError(IllegalStateException(exception.toString()))
        Truth.assertThat(actual.cause).isInstanceOf(expected.cause::class.java)
        Truth.assertThat(actual.cause).hasMessageThat().isEqualTo(expected.cause.message)

        coVerifyOrder {
            accountsCRUDRepository.getAccountListSync(userWalletId)
            accountsCRUDRepository.saveAccountsLocally(accountList)
            accountsCRUDRepository.syncTokens(userWalletId = userWalletId)
        }
    }

    @Test
    fun `when apply sorting for sorted and grouped list then correct args should be used`() = runTest {
        // Arrange
        val token1 = mockk<CryptoCurrency>(relaxed = true)
        val token2 = mockk<CryptoCurrency>(relaxed = true)
        val token3 = mockk<CryptoCurrency>(relaxed = true)

        val accountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = setOf(token1, token2, token3),
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        val sortedTokens = setOf(token2, token3, token1)
        val updatedAccountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = sortedTokens,
            sortType = TokensSortType.BALANCE,
            groupType = TokensGroupType.NETWORK,
        )

        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = mapOf(accountList.mainAccount to sortedTokens.toList()),
            isGroupedByNetwork = true,
            isSortedByBalance = true,
        )

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            accountsCRUDRepository.getAccountListSync(userWalletId)
            accountsCRUDRepository.saveAccountsLocally(updatedAccountList)
            accountsCRUDRepository.syncTokens(userWalletId)
        }
    }

    @Test
    fun `when apply sorting for unsorted and grouped list then correct args should be used`() = runTest {
        // Arrange
        val token1 = mockk<CryptoCurrency>(relaxed = true)
        val token2 = mockk<CryptoCurrency>(relaxed = true)
        val token3 = mockk<CryptoCurrency>(relaxed = true)

        val accountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = setOf(token1, token2, token3),
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        val sortedTokens = setOf(token2, token3, token1)
        val updatedAccountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = sortedTokens,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NETWORK,
        )

        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = mapOf(accountList.mainAccount to sortedTokens.toList()),
            isGroupedByNetwork = true,
            isSortedByBalance = false,
        )

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            accountsCRUDRepository.getAccountListSync(userWalletId)
            accountsCRUDRepository.saveAccountsLocally(updatedAccountList)
            accountsCRUDRepository.syncTokens(userWalletId)
        }
    }

    @Test
    fun `when apply sorting for sorted and ungrouped list then correct args should be used`() = runTest {
        // Arrange
        val token1 = mockk<CryptoCurrency>(relaxed = true)
        val token2 = mockk<CryptoCurrency>(relaxed = true)
        val token3 = mockk<CryptoCurrency>(relaxed = true)

        val accountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = setOf(token1, token2, token3),
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        val sortedTokens = setOf(token2, token3, token1)
        val updatedAccountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = sortedTokens,
            sortType = TokensSortType.BALANCE,
            groupType = TokensGroupType.NONE,
        )

        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = mapOf(accountList.mainAccount to sortedTokens.toList()),
            isGroupedByNetwork = false,
            isSortedByBalance = true,
        )

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            accountsCRUDRepository.getAccountListSync(userWalletId)
            accountsCRUDRepository.saveAccountsLocally(updatedAccountList)
            accountsCRUDRepository.syncTokens(userWalletId)
        }
    }

    @Test
    fun `when apply sorting for unsorted and ungrouped list then correct args should be used`() = runTest {
        // Arrange
        val token1 = mockk<CryptoCurrency>(relaxed = true)
        val token2 = mockk<CryptoCurrency>(relaxed = true)
        val token3 = mockk<CryptoCurrency>(relaxed = true)

        val accountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = setOf(token1, token2, token3),
            sortType = TokensSortType.BALANCE,
            groupType = TokensGroupType.NETWORK,
        )
        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        val sortedTokens = setOf(token2, token3, token1)
        val updatedAccountList = AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = sortedTokens,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )

        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = mapOf(accountList.mainAccount to sortedTokens.toList()),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            accountsCRUDRepository.getAccountListSync(userWalletId)
            accountsCRUDRepository.saveAccountsLocally(updatedAccountList)
            accountsCRUDRepository.syncTokens(userWalletId)
        }
    }

    @Test
    fun `when sorted tokens IDs do not contain all tokens IDs then error should be received`() = runTest {
        // Arrange
        val token1 = mockk<CryptoCurrency>(relaxed = true)
        val token2 = mockk<CryptoCurrency>(relaxed = true)
        val token3 = mockk<CryptoCurrency>(relaxed = true)

        val customAccount = Account.Crypto.Portfolio(
            accountId = AccountId.forCryptoPortfolio(userWalletId, DerivationIndex(1).getOrNull()!!),
            name = "Account 1",
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = 1,
            cryptoCurrencies = setOf(token1, token2, token3),
        )
            .getOrNull()!!

        val accountList = (AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = setOf(token1, token2, token3),
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        ) + customAccount).getOrNull()!!

        coEvery { accountsCRUDRepository.getAccountListSync(userWalletId) } returns accountList.some()

        val sortedTokensIdsByAccount: Map<Account, List<CryptoCurrency>> = mapOf(
            accountList.mainAccount to listOf(token3, token2, token1),
            customAccount to listOf(token2, token1), // missing token3
        )

        val updatedAccountList = AccountList(
            userWalletId = userWalletId,
            accounts = listOf(
                accountList.mainAccount.copy(cryptoCurrencies = setOf(token3, token2, token1)),
                customAccount, // unchanged due to error
            ),
            totalAccounts = accountList.totalAccounts,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
            .getOrNull()!!

        // Act
        val actual = useCase(
            sortedTokensIdsByAccount = sortedTokensIdsByAccount,
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        // Assert
        val expected = TokenListSortingError.UnableToSortAccounts(
            accountIds = listOf(customAccount.accountId.value),
        ).left()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerifyOrder {
            accountsCRUDRepository.getAccountListSync(userWalletId)
            accountsCRUDRepository.saveAccountsLocally(updatedAccountList)
            accountsCRUDRepository.syncTokens(userWalletId)
        }
    }
}