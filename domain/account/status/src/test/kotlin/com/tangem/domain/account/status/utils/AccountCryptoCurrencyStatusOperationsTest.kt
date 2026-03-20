package com.tangem.domain.account.status.utils

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyStatusOperations.getAccountCryptoCurrencyStatus
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
 * Tests for [AccountCryptoCurrencyStatusOperations].
 *
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountCryptoCurrencyStatusOperationsTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val userWalletId = UserWalletId("011")
    private val currency = cryptoCurrencyFactory.ethereum

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAccountCryptoCurrencyStatusByCurrency {

        @Test
        fun `returns None when AccountStatusList is null`() {
            val accountStatusList: AccountStatusList? = null
            val result = accountStatusList.getAccountCryptoCurrencyStatus(currency)
            assertNone(result)
        }

        @Test
        fun `returns None when currency is not found in AccountStatusList`() {
            val accountStatusList = createAccountStatusList(currencies = emptyList())
            val result = accountStatusList.getAccountCryptoCurrencyStatus(currency)
            assertNone(result)
        }

        @Test
        fun `returns Some when currency is found in AccountStatusList`() {
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val accountStatusList = createAccountStatusList(
                currencies = listOf(currency),
                currencyStatuses = listOf(currencyStatus),
            )
            val expected = AccountCryptoCurrencyStatus(
                account = accountStatusList.mainAccount.account,
                status = currencyStatus,
            )
            val result = accountStatusList.getAccountCryptoCurrencyStatus(currency)
            assertSome(result, expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAccountCryptoCurrencyStatusByCurrencyIdAndNetwork {

        @Test
        fun `returns None when AccountStatusList is null`() {
            val accountStatusList: AccountStatusList? = null
            val result = accountStatusList.getAccountCryptoCurrencyStatus(
                currencyId = currency.id,
                network = currency.network,
            )
            assertNone(result)
        }

        @Test
        fun `returns None when currency id is not found`() {
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
            val result = accountStatusList.getAccountCryptoCurrencyStatus(
                currencyId = otherCurrencyId,
                network = currency.network,
            )
            assertNone(result)
        }

        @Test
        fun `returns Some when currency id is found with null network`() {
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val accountStatusList = createAccountStatusList(
                currencies = listOf(currency),
                currencyStatuses = listOf(currencyStatus),
            )
            val expected = AccountCryptoCurrencyStatus(
                account = accountStatusList.mainAccount.account,
                status = currencyStatus,
            )
            val result = accountStatusList.getAccountCryptoCurrencyStatus(
                currencyId = currency.id,
                network = null,
            )
            assertSome(result, expected)
        }

        @Test
        fun `returns Some when currency id and network match`() {
            val currencyStatus = CryptoCurrencyStatus(
                currency = currency,
                value = CryptoCurrencyStatus.Loading,
            )
            val accountStatusList = createAccountStatusList(
                currencies = listOf(currency),
                currencyStatuses = listOf(currencyStatus),
            )
            val expected = AccountCryptoCurrencyStatus(
                account = accountStatusList.mainAccount.account,
                status = currencyStatus,
            )
            val result = accountStatusList.getAccountCryptoCurrencyStatus(
                currencyId = currency.id,
                network = currency.network,
            )
            assertSome(result, expected)
        }

        @Test
        fun `returns Some with first matching currency when multiple currencies exist`() {
            val currencies = cryptoCurrencyFactory.ethereumAndStellar
            val currencyStatuses = currencies.map {
                CryptoCurrencyStatus(currency = it, value = CryptoCurrencyStatus.Loading)
            }
            val accountStatusList = createAccountStatusList(
                currencies = currencies,
                currencyStatuses = currencyStatuses,
            )
            val targetCurrency = currencies.first()
            val expected = AccountCryptoCurrencyStatus(
                account = accountStatusList.mainAccount.account,
                status = currencyStatuses.first(),
            )
            val result = accountStatusList.getAccountCryptoCurrencyStatus(
                currencyId = targetCurrency.id,
                network = targetCurrency.network,
            )
            assertSome(result, expected)
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