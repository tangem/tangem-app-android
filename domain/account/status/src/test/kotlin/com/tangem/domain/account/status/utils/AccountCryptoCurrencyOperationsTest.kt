package com.tangem.domain.account.status.utils

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyOperations.getAccountCryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.assertNone
import com.tangem.test.core.assertSome
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [AccountCryptoCurrencyOperations].
 *
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountCryptoCurrencyOperationsTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val userWalletId = UserWalletId("011")
    private val currency = cryptoCurrencyFactory.ethereum

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAccountCryptoCurrencyByCurrency {

        @Test
        fun `returns None when AccountList is null`() {
            // Arrange
            val accountList: AccountList? = null
            // Act
            val result = accountList.getAccountCryptoCurrency(currency)
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns None when currency is not found in AccountList`() {
            // Arrange
            val accountList = AccountList.empty(userWalletId)
            // Act
            val result = accountList.getAccountCryptoCurrency(currency)
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns Some when currency is found in AccountList`() {
            // Arrange
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            val expected = AccountCryptoCurrency(
                account = accountList.mainAccount,
                cryptoCurrency = currency,
            )
            // Act
            val result = accountList.getAccountCryptoCurrency(currency)
            // Assert
            assertSome(result, expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetAccountCryptoCurrencyByCurrencyIdAndNetwork {

        @Test
        fun `returns None when AccountList is null`() {
            // Arrange
            val accountList: AccountList? = null
            // Act
            val result = accountList.getAccountCryptoCurrency(
                currencyId = currency.id,
                network = currency.network,
            )
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns None when currency id is not found`() {
            // Arrange
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            val otherCurrencyId = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("bitcoin"),
                suffix = CryptoCurrency.ID.Suffix.RawID("bitcoin"),
            )
            // Act
            val result = accountList.getAccountCryptoCurrency(
                currencyId = otherCurrencyId,
                network = currency.network,
            )
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns Some when currency id is found with null network`() {
            // Arrange
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            val expected = AccountCryptoCurrency(
                account = accountList.mainAccount,
                cryptoCurrency = currency,
            )
            // Act
            val result = accountList.getAccountCryptoCurrency(
                currencyId = currency.id,
                network = null,
            )
            // Assert
            assertSome(result, expected)
        }

        @Test
        fun `returns Some when currency id and network match`() {
            // Arrange
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            val expected = AccountCryptoCurrency(
                account = accountList.mainAccount,
                cryptoCurrency = currency,
            )
            // Act
            val result = accountList.getAccountCryptoCurrency(
                currencyId = currency.id,
                network = currency.network,
            )
            // Assert
            assertSome(result, expected)
        }

        @Test
        fun `returns Some with first matching currency when multiple currencies exist`() {
            // Arrange
            val currencies = cryptoCurrencyFactory.ethereumAndStellar
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = currencies,
            )
            val targetCurrency = currencies.first()
            val expected = AccountCryptoCurrency(
                account = accountList.mainAccount,
                cryptoCurrency = targetCurrency,
            )
            // Act
            val result = accountList.getAccountCryptoCurrency(
                currencyId = targetCurrency.id,
                network = targetCurrency.network,
            )
            // Assert
            assertSome(result, expected)
        }
    }
}