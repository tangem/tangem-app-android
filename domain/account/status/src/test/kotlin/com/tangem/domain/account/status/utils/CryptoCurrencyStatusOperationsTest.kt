package com.tangem.domain.account.status.utils

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.assertNone
import com.tangem.test.core.assertSome
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [CryptoCurrencyStatusOperations].
 *
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoCurrencyStatusOperationsTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val userWalletId = UserWalletId("011")
    private val currency = cryptoCurrencyFactory.ethereum

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCryptoCurrencyStatusFromAccountStatusListByCurrency {

        @Test
        fun `returns None when AccountStatusList is null`() {
            // Arrange
            val accountStatusList: AccountStatusList? = null
            // Act
            val result = accountStatusList.getCryptoCurrencyStatus(currency)
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns None when currency is not found in AccountStatusList`() {
            // Arrange
            val accountStatusList = createAccountStatusList(currencies = emptyList())
            // Act
            val result = accountStatusList.getCryptoCurrencyStatus(currency)
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns Some when currency is found in AccountStatusList`() {
            // Arrange
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val accountStatusList = createAccountStatusList(
                currencies = listOf(currency),
                currencyStatuses = listOf(currencyStatus),
            )
            // Act
            val result = accountStatusList.getCryptoCurrencyStatus(currency)
            // Assert
            assertSome(result, currencyStatus)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCryptoCurrencyStatusFromAccountStatusListByCurrencyIdAndNetwork {

        @Test
        fun `returns None when AccountStatusList is null`() {
            // Arrange
            val accountStatusList: AccountStatusList? = null
            // Act
            val result = accountStatusList.getCryptoCurrencyStatus(
                currencyId = currency.id,
                network = currency.network,
            )
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns None when currency id is not found`() {
            // Arrange
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val accountStatusList = createAccountStatusList(
                currencies = listOf(currency),
                currencyStatuses = listOf(currencyStatus),
            )
            val otherCurrencyId = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("bitcoin"),
                suffix = CryptoCurrency.ID.Suffix.RawID("bitcoin"),
            )
            // Act
            val result = accountStatusList.getCryptoCurrencyStatus(
                currencyId = otherCurrencyId,
                network = currency.network,
            )
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns Some when currency id is found with null network`() {
            // Arrange
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val accountStatusList = createAccountStatusList(
                currencies = listOf(currency),
                currencyStatuses = listOf(currencyStatus),
            )
            // Act
            val result = accountStatusList.getCryptoCurrencyStatus(
                currencyId = currency.id,
                network = null,
            )
            // Assert
            assertSome(result, currencyStatus)
        }

        @Test
        fun `returns Some when currency id and network match`() {
            // Arrange
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val accountStatusList = createAccountStatusList(
                currencies = listOf(currency),
                currencyStatuses = listOf(currencyStatus),
            )
            // Act
            val result = accountStatusList.getCryptoCurrencyStatus(
                currencyId = currency.id,
                network = currency.network,
            )
            // Assert
            assertSome(result, currencyStatus)
        }

        @Test
        fun `returns Some with first matching currency status when multiple currencies exist`() {
            // Arrange
            val currencies = cryptoCurrencyFactory.ethereumAndStellar
            val currencyStatuses = currencies.map {
                CryptoCurrencyStatus(currency = it, value = CryptoCurrencyStatus.Loading)
            }
            val accountStatusList = createAccountStatusList(
                currencies = currencies,
                currencyStatuses = currencyStatuses,
            )
            val targetCurrency = currencies.first()
            val expectedStatus = currencyStatuses.first()
            // Act
            val result = accountStatusList.getCryptoCurrencyStatus(
                currencyId = targetCurrency.id,
                network = targetCurrency.network,
            )
            // Assert
            assertSome(result, expectedStatus)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCryptoCurrencyStatusFromCryptoPortfolio {

        @Test
        fun `returns None when currency id is not found in CryptoPortfolio`() {
            // Arrange
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val account = Account.CryptoPortfolio.createMainAccount(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = listOf(currencyStatus),
                ),
                priceChangeLce = lceLoading(),
            )
            val otherCurrencyId = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("bitcoin"),
                suffix = CryptoCurrency.ID.Suffix.RawID("bitcoin"),
            )
            // Act
            val result = accountStatus.getCryptoCurrencyStatus(otherCurrencyId)
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns Some when currency id is found in CryptoPortfolio`() {
            // Arrange
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val account = Account.CryptoPortfolio.createMainAccount(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = listOf(currencyStatus),
                ),
                priceChangeLce = lceLoading(),
            )
            // Act
            val result = accountStatus.getCryptoCurrencyStatus(currency.id)
            // Assert
            assertSome(result, currencyStatus)
        }

        @Test
        fun `returns None when CryptoPortfolio has empty token list`() {
            // Arrange
            val account = Account.CryptoPortfolio.createMainAccount(
                userWalletId = userWalletId,
                cryptoCurrencies = emptyList(),
            )
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Empty,
                priceChangeLce = lceLoading(),
            )
            // Act
            val result = accountStatus.getCryptoCurrencyStatus(currency.id)
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns Some with matching currency status when multiple currencies exist`() {
            // Arrange
            val currencies = cryptoCurrencyFactory.ethereumAndStellar
            val currencyStatuses = currencies.map {
                CryptoCurrencyStatus(currency = it, value = CryptoCurrencyStatus.Loading)
            }
            val account = Account.CryptoPortfolio.createMainAccount(
                userWalletId = userWalletId,
                cryptoCurrencies = currencies,
            )
            val accountStatus = AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = currencyStatuses,
                ),
                priceChangeLce = lceLoading(),
            )
            val targetCurrency = currencies.last()
            val expectedStatus = currencyStatuses.last()
            // Act
            val result = accountStatus.getCryptoCurrencyStatus(targetCurrency.id)
            // Assert
            assertSome(result, expectedStatus)
        }
    }

    private fun createAccountStatusList(
        currencies: List<CryptoCurrency>,
        currencyStatuses: List<CryptoCurrencyStatus> = emptyList(),
    ): AccountStatusList {
        val account = Account.CryptoPortfolio.createMainAccount(
            userWalletId = userWalletId,
            cryptoCurrencies = currencies,
        )
        val tokenList = if (currencyStatuses.isEmpty()) {
            TokenList.Empty
        } else {
            TokenList.Ungrouped(
                totalFiatBalance = TotalFiatBalance.Loading,
                sortedBy = TokensSortType.NONE,
                currencies = currencyStatuses,
            )
        }
        val accountStatus = AccountStatus.CryptoPortfolio(
            account = account,
            tokenList = tokenList,
            priceChangeLce = lceLoading(),
        )
        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(accountStatus),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }
}