package com.tangem.domain.account.status.utils

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getTokens
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.assertNone
import com.tangem.test.core.assertSome
import com.google.common.truth.Truth.assertThat
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTokensFromAccountList {

        @Test
        fun `returns empty list when no tokens exist in the same network as the coin`() {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(coin),
            )
            // Act
            val result = accountList.getTokens(coin)
            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `returns empty list when only coins exist in AccountList`() {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val otherCoin = cryptoCurrencyFactory.stellar
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(coin, otherCoin),
            )
            // Act
            val result = accountList.getTokens(coin)
            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `returns tokens when tokens exist in the same network`() {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val token = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(coin, token),
            )
            // Act
            val result = accountList.getTokens(coin)
            // Assert
            assertThat(result).containsExactly(token)
        }

        @Test
        fun `returns multiple tokens when multiple tokens exist in the same network`() {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val token1 = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)
            val token2 = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(coin, token1, token2),
            )
            // Act
            val result = accountList.getTokens(coin)
            // Assert
            assertThat(result).containsExactly(token1, token2)
        }

        @Test
        fun `returns empty list when tokens exist only in other networks`() {
            // Arrange
            val ethereumCoin = cryptoCurrencyFactory.ethereum
            val stellarCoin = cryptoCurrencyFactory.stellar
            val stellarToken = cryptoCurrencyFactory.createToken(Blockchain.Stellar)
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(ethereumCoin, stellarCoin, stellarToken),
            )
            // Act
            val result = accountList.getTokens(ethereumCoin)
            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `returns only tokens from the same network when tokens exist in multiple networks`() {
            // Arrange
            val ethereumCoin = cryptoCurrencyFactory.ethereum
            val ethereumToken = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)
            val stellarCoin = cryptoCurrencyFactory.stellar
            val stellarToken = cryptoCurrencyFactory.createToken(Blockchain.Stellar)
            val accountList = AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(ethereumCoin, ethereumToken, stellarCoin, stellarToken),
            )
            // Act
            val result = accountList.getTokens(ethereumCoin)
            // Assert
            assertThat(result).containsExactly(ethereumToken)
        }

        @Test
        fun `returns empty list when AccountList is empty`() {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val accountList = AccountList.empty(userWalletId)
            // Act
            val result = accountList.getTokens(coin)
            // Assert
            assertThat(result).isEmpty()
        }
    }
}