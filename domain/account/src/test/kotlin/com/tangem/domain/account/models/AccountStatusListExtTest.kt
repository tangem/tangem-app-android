package com.tangem.domain.account.models

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountStatusListExtTest {

    @Test
    fun `GIVEN no accounts WHEN hasMultiCurrencyAccount THEN returns false`() {
        val accountList = createAccountStatusList(accountStatuses = emptyList())

        val result = accountList.hasMultiCurrencyAccount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN only Payment accounts WHEN hasMultiCurrencyAccount THEN returns false`() {
        val accountList = createAccountStatusList(
            accountStatuses = listOf(mockk<AccountStatus.Payment>()),
        )

        val result = accountList.hasMultiCurrencyAccount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN CryptoPortfolio with single currency WHEN hasMultiCurrencyAccount THEN returns false`() {
        val accountList = createAccountStatusList(
            accountStatuses = listOf(cryptoPortfolioWithCurrencies(count = 1)),
        )

        val result = accountList.hasMultiCurrencyAccount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN CryptoPortfolio with no currencies WHEN hasMultiCurrencyAccount THEN returns false`() {
        val accountList = createAccountStatusList(
            accountStatuses = listOf(cryptoPortfolioWithCurrencies(count = 0)),
        )

        val result = accountList.hasMultiCurrencyAccount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN CryptoPortfolio with multiple currencies WHEN hasMultiCurrencyAccount THEN returns true`() {
        val accountList = createAccountStatusList(
            accountStatuses = listOf(cryptoPortfolioWithCurrencies(count = 2)),
        )

        val result = accountList.hasMultiCurrencyAccount()

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN mix of single and multi currency portfolios WHEN hasMultiCurrencyAccount THEN returns true`() {
        val accountList = createAccountStatusList(
            accountStatuses = listOf(
                cryptoPortfolioWithCurrencies(count = 1),
                cryptoPortfolioWithCurrencies(count = 3),
            ),
        )

        val result = accountList.hasMultiCurrencyAccount()

        assertThat(result).isTrue()
    }

    private fun createAccountStatusList(accountStatuses: List<AccountStatus>): AccountStatusList {
        return mockk {
            every { this@mockk.accountStatuses } returns accountStatuses
        }
    }

    private fun cryptoPortfolioWithCurrencies(count: Int): AccountStatus.CryptoPortfolio {
        val tokenList = mockk<TokenList> {
            every { flattenCurrencies() } returns List(count) { mockk<CryptoCurrencyStatus>() }
        }
        return mockk {
            every { this@mockk.tokenList } returns tokenList
        }
    }
}