package com.tangem.features.earn.lending.impl.session

import arrow.core.getOrElse
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class LendingAccountNamesTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val ethereum = MockCryptoCurrencyFactory().ethereum
    private val ethereumStatus = CryptoCurrencyStatus(currency = ethereum, value = CryptoCurrencyStatus.Loading)
    private val supplier: SingleAccountStatusListSupplier = mockk()
    private val accountNames = LendingAccountNames(supplier)

    @Test
    fun `GIVEN the coin lives in a named account WHEN name is asked THEN it is that name`() = runTest {
        // Arrange
        val savings = AccountName("Savings").getOrElse { error("The name is valid") }
        arrangeAccounts(portfolio(name = AccountName.DefaultMain), portfolio(name = savings, holdsEthereum = true))

        // Act
        val actual = accountNames.nameOf(userWalletId, ethereum.id)

        // Assert
        assertThat(actual).isEqualTo(stringReference("Savings"))
    }

    @Test
    fun `GIVEN the coin lives in the main account WHEN name is asked THEN it is the main account title`() = runTest {
        // Arrange
        arrangeAccounts(portfolio(name = AccountName.DefaultMain, holdsEthereum = true))

        // Act
        val actual = accountNames.nameOf(userWalletId, ethereum.id)

        // Assert
        assertThat(actual).isEqualTo(MAIN_ACCOUNT)
    }

    @Test
    fun `GIVEN no account holds the coin WHEN name is asked THEN it falls back to the main account title`() = runTest {
        // Arrange
        arrangeAccounts(portfolio(name = AccountName.DefaultMain))

        // Act
        val actual = accountNames.nameOf(userWalletId, ethereum.id)

        // Assert
        assertThat(actual).isEqualTo(MAIN_ACCOUNT)
    }

    @Test
    fun `GIVEN the accounts never arrive WHEN name is asked THEN it falls back to the main account title`() = runTest {
        // Arrange
        every { supplier(userWalletId) } returns MutableSharedFlow()

        // Act
        val actual = accountNames.nameOf(userWalletId, ethereum.id)

        // Assert
        assertThat(actual).isEqualTo(MAIN_ACCOUNT)
    }

    private fun arrangeAccounts(vararg statuses: AccountStatus.CryptoPortfolio) {
        val list: AccountStatusList = mockk { every { accountStatuses } returns statuses.toList() }
        every { supplier(userWalletId) } returns flowOf(list)
    }

    private fun portfolio(name: AccountName, holdsEthereum: Boolean = false): AccountStatus.CryptoPortfolio {
        val account: Account.CryptoPortfolio = mockk { every { accountName } returns name }
        return mockk {
            every { this@mockk.account } returns account
            every { flattenCurrencies() } returns if (holdsEthereum) listOf(ethereumStatus) else emptyList()
        }
    }

    private companion object {
        val MAIN_ACCOUNT = resourceReference(R.string.account_main_account_title)
    }
}