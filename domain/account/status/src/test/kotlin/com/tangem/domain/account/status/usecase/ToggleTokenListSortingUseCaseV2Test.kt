package com.tangem.domain.account.status.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.utils.createGroupedByNetwork
import com.tangem.domain.account.status.utils.createStatus
import com.tangem.domain.account.status.utils.createUngrouped
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToggleTokenListSortingUseCaseV2Test {

    private val useCase = ToggleTokenListSortingUseCaseV2(dispatchers = TestingCoroutineDispatcherProvider())

    private val userWalletId = UserWalletId("011")
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    @Test
    fun `when list is empty then error should be received`() = runTest {
        // Arrange
        val accountStatusList = AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = emptyList(),
            totalAccounts = 0,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Failed,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )

        // Act
        val actual = useCase(accountStatusList)

        // Assert
        val expected = TokenListSortingError.TokenListIsEmpty.left()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when totalFiatBalance is loading then error should be received`() = runTest {
        // Arrange
        val tokenList = TokenList.Ungrouped(
            totalFiatBalance = TotalFiatBalance.Loading,
            sortedBy = TokensSortType.NONE,
            currencies = listOf(mockk(relaxed = true)),
        )

        val accountStatusList = createAccountStatusList(tokenList)

        // Act
        val actual = useCase(accountStatusList)

        // Assert
        val expected = TokenListSortingError.TokenListIsLoading.left()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when list is grouped and unsorted then grouped and sorted list should be received`() = runTest {
        // Arrange
        val tokenList = createGroupedByNetwork(
            statuses = listOf(
                createStatus(currency = cryptoCurrencyFactory.cardano, fiatAmount = BigDecimal.ZERO),
                createStatus(currency = cryptoCurrencyFactory.chia, fiatAmount = BigDecimal.TEN),
                createStatus(currency = cryptoCurrencyFactory.ethereum, fiatAmount = BigDecimal.ONE),
            ),
        )

        val accountStatusList = createAccountStatusList(tokenList)

        val updatedTokenList = createGroupedByNetwork(
            statuses = listOf(
                createStatus(currency = cryptoCurrencyFactory.chia, fiatAmount = BigDecimal.TEN),
                createStatus(currency = cryptoCurrencyFactory.ethereum, fiatAmount = BigDecimal.ONE),
                createStatus(currency = cryptoCurrencyFactory.cardano, fiatAmount = BigDecimal.ZERO),
            ),
            sortedBy = TokensSortType.BALANCE,
        )

        // Act
        val actual = useCase(accountStatusList)

        // Assert
        val expected = createAccountStatusList(updatedTokenList).right()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `when list is ungrouped and unsorted then ungrouped and sorted list should be received`() = runTest {
        // Arrange
        val tokenList = createUngrouped(
            statuses = listOf(
                createStatus(currency = cryptoCurrencyFactory.cardano, fiatAmount = BigDecimal.ZERO),
                createStatus(currency = cryptoCurrencyFactory.chia, fiatAmount = BigDecimal.TEN),
                createStatus(currency = cryptoCurrencyFactory.ethereum, fiatAmount = BigDecimal.ONE),
            ),
        )

        val accountStatusList = createAccountStatusList(tokenList)

        val updatedTokenList = createUngrouped(
            statuses = listOf(
                createStatus(currency = cryptoCurrencyFactory.chia, fiatAmount = BigDecimal.TEN),
                createStatus(currency = cryptoCurrencyFactory.ethereum, fiatAmount = BigDecimal.ONE),
                createStatus(currency = cryptoCurrencyFactory.cardano, fiatAmount = BigDecimal.ZERO),
            ),
            sortedBy = TokensSortType.BALANCE,
        )

        // Act
        val actual = useCase(accountStatusList)

        // Assert
        val expected = createAccountStatusList(updatedTokenList).right()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun createAccountStatusList(tokenList: TokenList): AccountStatusList {
        val accountStatus = AccountStatus.Crypto.Portfolio(
            account = Account.Crypto.Portfolio.createMainAccount(userWalletId),
            tokenList = tokenList,
            priceChangeLce = Unit.lceError(),
        )

        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(accountStatus),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = tokenList.totalFiatBalance,
            sortType = tokenList.sortedBy,
            groupType = TokensGroupType.NONE,
        )
    }
}