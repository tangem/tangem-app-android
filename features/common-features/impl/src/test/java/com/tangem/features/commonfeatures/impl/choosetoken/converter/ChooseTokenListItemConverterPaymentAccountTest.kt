package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter
import com.tangem.features.commonfeatures.api.choosetoken.model.TokenListUMData
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.choosetoken.model.ClickIntents
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ChooseTokenListItemConverterPaymentAccountTest {

    private val polygonToken = MockAccounts.createPaymentAccountToken()
    private val tronToken = MockAccounts.createPaymentAccountToken(
        rawId = "usdc-tron",
        networkId = "tron",
        networkName = "Tron",
        networkCurrencySymbol = "TRX",
    )

    @Test
    fun `GIVEN account with two enabled networks WHEN convert THEN both tokens are listed`() {
        // Arrange
        val paymentAccount = createPaymentAccount(
            networks = listOf(availableNetwork(polygonToken), availableNetwork(tronToken)),
        )

        // Act
        val paymentPortfolio = createConverter(paymentAccount).convert().paymentPortfolio()

        // Assert
        assertThat(paymentPortfolio.tokensItemsList.map { it.state.id })
            .containsExactly(polygonToken.id.value, tronToken.id.value)
            .inOrder()
    }

    @Test
    fun `GIVEN multi token disabled WHEN convert THEN only the account currency is listed`() {
        // Arrange
        val paymentAccount = createPaymentAccount(
            networks = listOf(availableNetwork(polygonToken), availableNetwork(tronToken)),
        )

        // Act
        val paymentPortfolio = createConverter(paymentAccount, isMultiTokenEnabled = false)
            .convert()
            .paymentPortfolio()

        // Assert
        assertThat(paymentPortfolio.tokensItemsList.map { it.state.id })
            .containsExactly(polygonToken.id.value)
    }

    @Test
    fun `GIVEN token filter rejects a network WHEN convert THEN rejected token is not listed`() {
        // Arrange — this is the hook a feature uses to narrow the list (e.g. swap excluding the picked pair).
        val paymentAccount = createPaymentAccount(
            networks = listOf(availableNetwork(polygonToken), availableNetwork(tronToken)),
        )
        val converter = createConverter(
            paymentAccount = paymentAccount,
            tokenFilter = { _, status -> status.currency.network == polygonToken.network },
        )

        // Act
        val paymentPortfolio = converter.convert().paymentPortfolio()

        // Assert
        assertThat(paymentPortfolio.tokensItemsList.map { it.state.id })
            .containsExactly(polygonToken.id.value)
    }

    @Test
    fun `GIVEN token filter rejects a network WHEN convert THEN the header counts only the shown tokens`() {
        // Arrange
        val paymentAccount = createPaymentAccount(
            networks = listOf(availableNetwork(polygonToken), availableNetwork(tronToken)),
        )
        val converter = createConverter(
            paymentAccount = paymentAccount,
            tokenFilter = { _, status -> status.currency.network == polygonToken.network },
        )

        // Act
        val paymentPortfolio = converter.convert().paymentPortfolio()

        // Assert
        val subtitle = paymentPortfolio.tokenItemUM.subtitleState as TokenItemState.SubtitleState.TextContent
        assertThat(subtitle.value).isEqualTo(
            pluralReference(R.plurals.common_tokens_count, count = 1, formatArgs = wrappedList(1)),
        )
    }

    private fun createPaymentAccount(networks: List<PaymentNetworkStatus>): AccountStatus.Payment =
        MockAccounts.createPaymentAccountStatus(cryptoCurrency = polygonToken, networks = networks)

    private fun availableNetwork(token: CryptoCurrency.Token): PaymentNetworkStatus.Available =
        PaymentNetworkStatus.Available(
            network = token.network,
            depositAddress = DEPOSIT_ADDRESS,
            cryptoCurrencyStatuses = listOf(createCurrencyStatus(token)),
        )

    private fun createCurrencyStatus(token: CryptoCurrency.Token): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = token,
        value = CryptoCurrencyStatus.NoQuote(
            amount = BigDecimal.ONE,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    type = NetworkAddress.Address.Type.Primary,
                    value = DEPOSIT_ADDRESS,
                ),
            ),
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private fun createConverter(
        paymentAccount: AccountStatus.Payment,
        isMultiTokenEnabled: Boolean = true,
        tokenFilter: (AccountStatus, CryptoCurrencyStatus) -> Boolean = { _, _ -> true },
    ) = ChooseTokenListItemConverter(
        appCurrency = AppCurrency.Default,
        params = TokenConverterParams.Account(
            accountList = createAccountList(paymentAccount),
            expandedAccounts = setOf(paymentAccount.account.accountId),
        ),
        config = ConverterConfig(
            clickIntents = mockk(relaxed = true),
            searchQuery = SearchQuery.Empty,
            tokenFilter = tokenFilter,
            isShowPaymentAccount = true,
            isPaymentAccountMultiTokenEnabled = isMultiTokenEnabled,
            balanceFilter = BalanceFilter.All,
        ),
    )

    private fun createAccountList(paymentAccount: AccountStatus.Payment) = AccountStatusList(
        userWalletId = MockAccounts.userWalletId,
        accountStatuses = listOf(paymentAccount),
        totalAccounts = 1,
        totalArchivedAccounts = 0,
        totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
        sortType = TokensSortType.NONE,
        groupType = TokensGroupType.NONE,
    )

    private fun TokenListUMData.paymentPortfolio(): TokensListItemUM.Portfolio {
        val accountList = this as TokenListUMData.AccountList
        return accountList.tokensList.filterIsInstance<TokensListItemUM.Portfolio>().single()
    }

    private companion object {
        const val DEPOSIT_ADDRESS = "0xdeposit"
    }
}