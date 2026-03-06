package com.tangem.domain.account.status.utils

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.assertNone
import com.tangem.test.core.assertSome
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [CryptoCurrencyOperations].
 *
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoCurrencyOperationsTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val userWalletId = UserWalletId("011")
    private val currency = cryptoCurrencyFactory.ethereum

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCryptoCurrencyFromAccountListByCurrency {

        @Test
        fun `returns None when currency is not found in AccountList`() {
            // Arrange
            val accountList = AccountList.empty(userWalletId)
            // Act
            val result = accountList.getCryptoCurrency(currency)
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
            // Act
            val result = accountList.getCryptoCurrency(currency)
            // Assert
            assertSome(result, currency)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCryptoCurrencyFromAccountListByCurrencyIdAndNetwork {

        @Test
        fun `returns None when AccountList is null`() {
            // Arrange
            val accountList: AccountList? = null
            // Act
            val result = accountList.getCryptoCurrency(
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
            val result = accountList.getCryptoCurrency(
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
            // Act
            val result = accountList.getCryptoCurrency(
                currencyId = currency.id,
                network = null,
            )
            // Assert
            assertSome(result, currency)
        }

        @Test
        fun `returns Some when currency id and network match`() {
            // Arrange
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            // Act
            val result = accountList.getCryptoCurrency(
                currencyId = currency.id,
                network = currency.network,
            )
            // Assert
            assertSome(result, currency)
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
            // Act
            val result = accountList.getCryptoCurrency(
                currencyId = targetCurrency.id,
                network = targetCurrency.network,
            )
            // Assert
            assertSome(result, targetCurrency)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCryptoCurrencyFromCryptoPortfolio {

        @Test
        fun `returns None when currency id is not found in CryptoPortfolio`() {
            // Arrange
            val account = Account.CryptoPortfolio.createMainAccount(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            val otherCurrencyId = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("bitcoin"),
                suffix = CryptoCurrency.ID.Suffix.RawID("bitcoin"),
            )
            // Act
            val result = account.getCryptoCurrency(otherCurrencyId)
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns Some when currency id is found in CryptoPortfolio`() {
            // Arrange
            val account = Account.CryptoPortfolio.createMainAccount(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(currency),
            )
            // Act
            val result = account.getCryptoCurrency(currency.id)
            // Assert
            assertSome(result, currency)
        }

        @Test
        fun `returns None when CryptoPortfolio has no currencies`() {
            // Arrange
            val account = Account.CryptoPortfolio.createMainAccount(
                userWalletId = userWalletId,
                cryptoCurrencies = emptyList(),
            )
            // Act
            val result = account.getCryptoCurrency(currency.id)
            // Assert
            assertNone(result)
        }

        @Test
        fun `returns Some with matching currency when multiple currencies exist`() {
            // Arrange
            val currencies = cryptoCurrencyFactory.ethereumAndStellar
            val account = Account.CryptoPortfolio.createMainAccount(
                userWalletId = userWalletId,
                cryptoCurrencies = currencies,
            )
            val targetCurrency = currencies.last()
            // Act
            val result = account.getCryptoCurrency(targetCurrency.id)
            // Assert
            assertSome(result, targetCurrency)
        }
    }
}