package com.tangem.features.txhistory.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.mock.MockAccounts
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Unit tests for [buildOwnAccountAddressMapAllNetworks]: the `address -> account` map the whole history lookup rests
 * on. Pins that a currency is registered under every address it is watched under — its other address types and the
 * XPUB-derived addresses of a dynamic-addresses network — so a transfer between two of the user's own portfolios is
 * recognised whichever of their addresses it landed on ([REDACTED_TASK_KEY]).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BuildOwnAccountAddressMapTest {

    private val bitcoin = MockCryptoCurrencyFactory().bitcoin
    private val bitcoinRawId = bitcoin.network.id.rawId

    @Test
    fun `GIVEN currency with several address types WHEN build THEN every address maps to the account`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1)
        val lists = listOf(
            accountStatusList(
                personalStatus(account, currencyStatus(SEGWIT_ADDRESS, otherAddresses = listOf(LEGACY_ADDRESS))),
            ),
        )

        // Act
        val actual = buildOwnAccountAddressMapAllNetworks(lists) { _, _ -> emptyList() }

        // Assert
        assertThat(actual).isEqualTo(
            mapOf(bitcoinRawId to mapOf(SEGWIT_ADDRESS to account, LEGACY_ADDRESS to account)),
        )
    }

    @Test
    fun `GIVEN dynamic addresses used on a network WHEN build THEN they map to the owning account`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1)
        val lists = listOf(accountStatusList(personalStatus(account, currencyStatus(SEGWIT_ADDRESS))))

        // Act
        val actual = buildOwnAccountAddressMapAllNetworks(lists) { _, _ -> listOf(DYNAMIC_ADDRESS) }

        // Assert
        assertThat(actual).isEqualTo(
            mapOf(bitcoinRawId to mapOf(SEGWIT_ADDRESS to account, DYNAMIC_ADDRESS to account)),
        )
    }

    @Test
    fun `GIVEN two accounts on one network WHEN build THEN each dynamic address maps to its own account`() = runTest {
        // Arrange
        val sender = MockAccounts.createAccount(derivationIndex = 1)
        val recipient = MockAccounts.createAccount(derivationIndex = 2)
        val recipientPath = Network.DerivationPath.Card(RECIPIENT_PATH)
        val recipientCurrency = bitcoin.copy(
            network = bitcoin.network.copy(
                id = bitcoin.network.id.copy(derivationPath = recipientPath),
                derivationPath = recipientPath,
            ),
        )
        val lists = listOf(
            accountStatusList(
                personalStatus(sender, currencyStatus(SEGWIT_ADDRESS)),
                personalStatus(recipient, currencyStatus(LEGACY_ADDRESS, currency = recipientCurrency)),
            ),
        )

        // Act
        val actual = buildOwnAccountAddressMapAllNetworks(lists) { _, network ->
            if (network.derivationPath.value == RECIPIENT_PATH) {
                listOf(RECIPIENT_DYNAMIC_ADDRESS)
            } else {
                listOf(DYNAMIC_ADDRESS)
            }
        }

        // Assert
        assertThat(actual).isEqualTo(
            mapOf(
                bitcoinRawId to mapOf(
                    SEGWIT_ADDRESS to sender,
                    DYNAMIC_ADDRESS to sender,
                    LEGACY_ADDRESS to recipient,
                    RECIPIENT_DYNAMIC_ADDRESS to recipient,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN currency without a network address WHEN build THEN it is not registered`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1)
        val lists = listOf(accountStatusList(personalStatus(account, loadingCurrencyStatus())))

        // Act
        val actual = buildOwnAccountAddressMapAllNetworks(lists) { _, _ -> emptyList() }

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN payment account WHEN build THEN its deposit address maps to the payment account`() = runTest {
        // Arrange
        val status = MockAccounts.createPaymentAccountStatus()
        val lists = listOf(accountStatusList(status))

        // Act
        val actual = buildOwnAccountAddressMapAllNetworks(lists) { _, _ -> emptyList() }

        // Assert
        val token = MockAccounts.createPaymentAccountToken()
        assertThat(actual).isEqualTo(
            mapOf(token.network.id.rawId to mapOf(DEPOSIT_ADDRESS to Account.Payment(MockAccounts.userWalletId))),
        )
    }

    private fun accountStatusList(
        vararg statuses: AccountStatus,
        userWalletId: UserWalletId = MockAccounts.userWalletId,
    ): AccountStatusList = AccountStatusList(
        userWalletId = userWalletId,
        accountStatuses = statuses.toList(),
        totalAccounts = statuses.size,
        totalArchivedAccounts = 0,
        totalFiatBalance = TotalFiatBalance.Loading,
        sortType = TokensSortType.NONE,
        groupType = TokensGroupType.NONE,
    )

    private fun personalStatus(account: Account.Personal, currencyStatus: CryptoCurrencyStatus) =
        AccountStatus.Personal(
            account = account,
            tokenList = TokenList.Ungrouped(
                totalFiatBalance = TotalFiatBalance.Loading,
                sortedBy = TokensSortType.NONE,
                currencies = listOf(currencyStatus),
            ),
            priceChangeLce = Unit.lceError(),
        )

    private fun currencyStatus(
        defaultAddress: String,
        otherAddresses: List<String> = emptyList(),
        currency: CryptoCurrency = bitcoin,
    ): CryptoCurrencyStatus {
        val default = NetworkAddress.Address(value = defaultAddress, type = NetworkAddress.Address.Type.Primary)
        val secondary = otherAddresses.map {
            NetworkAddress.Address(value = it, type = NetworkAddress.Address.Type.Secondary)
        }

        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal.ONE,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = if (secondary.isEmpty()) {
                    NetworkAddress.Single(defaultAddress = default)
                } else {
                    NetworkAddress.Selectable(
                        defaultAddress = default,
                        availableAddresses = (listOf(default) + secondary).toSet(),
                    )
                },
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )
    }

    private fun loadingCurrencyStatus(currency: CryptoCurrency = bitcoin) = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Loading,
    )

    private companion object {
        const val SEGWIT_ADDRESS = "bc1q2h9dg7krrys54u53wrhnts4gncmv99dg4uqglw"
        const val LEGACY_ADDRESS = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2"
        const val DYNAMIC_ADDRESS = "bc1qynhdp54nywsj8sk0h86lmk7aalkgyxryyntlpv"
        const val RECIPIENT_DYNAMIC_ADDRESS = "bc1qh5fhjnddqk5zjx38gz727dd9nu5ht2sww50k7f"
        const val RECIPIENT_PATH = "m/84'/0'/1'/0/0"
        const val DEPOSIT_ADDRESS = "0xdeposit"
    }
}