package com.tangem.features.onramp.tokenlist.entity.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.entity.AccountAvailabilityUM
import com.tangem.features.onramp.tokenlist.entity.AccountCurrencyUM
import com.tangem.features.onramp.tokenlist.entity.OnrampTokenGroupUM
import com.tangem.features.onramp.tokenlist.entity.OnrampTokenRowUM
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class OnrampTokenGroupConverterTest {

    private val ethereum = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
    private val reason = stringReference("Unavailable")
    private val account = MockAccounts.createAccount(derivationIndex = 1, name = "Savings")

    private val converter = OnrampTokenGroupConverter(
        appCurrency = AppCurrency.Default,
        unavailableErrorText = reason,
        onItemClick = { _, _ -> },
    )

    @Test
    fun `GIVEN accounts mode WHEN convert THEN one group per account with account header`() {
        // Arrange
        val accountList = listOf(AccountAvailabilityUM(account, listOf(AccountCurrencyUM(true, loadedStatus()))))

        // Act
        val actual = converter.convert(accountList = accountList, isAccountsMode = true)

        // Assert
        val group = actual.single()
        assertThat(group.id).isEqualTo(account.accountId.value)
        val header = (group.header as OnrampTokenGroupUM.Header.Account).headerRowUM
        assertThat(header.id).isEqualTo(account.accountId.value)
        assertThat(header.title).isEqualTo(stringReference("Savings"))
        assertThat(header.subtitle).isEqualTo(
            pluralReference(R.plurals.common_tokens_count, count = 1, formatArgs = wrappedList(1)),
        )
        assertThat(header.startIconUM).isNotNull()
        assertThat(group.rows.single()).isInstanceOf(OnrampTokenRowUM.Available::class.java)
    }

    @Test
    fun `GIVEN accounts mode off WHEN convert THEN single available tokens group`() {
        // Arrange
        val accountList = listOf(AccountAvailabilityUM(account, listOf(AccountCurrencyUM(true, loadedStatus()))))

        // Act
        val actual = converter.convert(accountList = accountList, isAccountsMode = false)

        // Assert
        val group = actual.single()
        assertThat(group.header).isEqualTo(
            OnrampTokenGroupUM.Header.Section(resourceReference(R.string.exchange_tokens_available_tokens_header)),
        )
    }

    @Test
    fun `GIVEN no currencies AND accounts mode off WHEN convert THEN no groups`() {
        // Act
        val actual = converter.convert(accountList = listOf(AccountAvailabilityUM(account, emptyList())), isAccountsMode = false)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN available currency WHEN convert THEN DS3 token row content`() {
        // Arrange
        val accountList = listOf(AccountAvailabilityUM(account, listOf(AccountCurrencyUM(true, loadedStatus()))))

        // Act
        val row = converter.convert(accountList, isAccountsMode = true).single().rows.single() as OnrampTokenRowUM.Available

        // Assert
        assertThat(row.state).isInstanceOf(TangemTokenRow.State.Content::class.java)
        assertThat(row.id).isEqualTo(ethereum.id.value)
    }

    @Test
    fun `GIVEN unavailable currency WHEN convert THEN dimmed row with reason and balances`() {
        // Arrange
        val accountList = listOf(AccountAvailabilityUM(account, listOf(AccountCurrencyUM(false, loadedStatus()))))

        // Act
        val row = converter.convert(accountList, isAccountsMode = true).single().rows.single() as OnrampTokenRowUM.Unavailable

        // Assert
        assertThat(row.title).isEqualTo(stringReference(ethereum.name))
        assertThat(row.reason).isEqualTo(reason)
        assertThat(row.fiatBalance).isNotNull()
        assertThat(row.cryptoBalance).isNotNull()
    }

    @Test
    fun `GIVEN unavailable unreachable currency WHEN convert THEN no balances`() {
        // Arrange
        val unreachable = CryptoCurrencyStatus(
            currency = ethereum,
            value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
        )
        val accountList = listOf(AccountAvailabilityUM(account, listOf(AccountCurrencyUM(false, unreachable))))

        // Act
        val row = converter.convert(accountList, isAccountsMode = true).single().rows.single() as OnrampTokenRowUM.Unavailable

        // Assert
        assertThat(row.fiatBalance).isNull()
        assertThat(row.cryptoBalance).isNull()
    }

    private fun loadedStatus() = CryptoCurrencyStatus(
        currency = ethereum,
        value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.TEN,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )
}