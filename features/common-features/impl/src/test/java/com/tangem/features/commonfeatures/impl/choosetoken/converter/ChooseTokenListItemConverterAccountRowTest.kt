package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.toAvatarState
import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.PaymentAccountTokens
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter
import com.tangem.features.commonfeatures.api.choosetoken.model.TokenListUMData
import com.tangem.features.commonfeatures.api.choosetoken.model.TokenRowItemUM
import com.tangem.features.commonfeatures.impl.R
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ChooseTokenListItemConverterAccountRowTest {

    private val token = MockAccounts.createPaymentAccountToken()

    @Test
    fun `GIVEN redesigned rows enabled WHEN convert personal account THEN its row is built`() {
        // Arrange
        val personal = personalStatus()
        val converter = createConverter(accounts = listOf(personal), isRedesignedTokenRowsEnabled = true)

        // Act
        val data = converter.convert() as TokenListUMData.AccountList

        // Assert
        val portfolio = data.tokensList.single()
        val row = data.accountRows.getValue(portfolio.id)
        assertThat(row.avatar).isEqualTo(personal.account.toAvatarState())
        val state = row.state as TangemAccountRow.State.Content
        assertThat(state.title).isEqualTo(stringReference(ACCOUNT_NAME))
        assertThat(state.label).isEqualTo(
            pluralReference(R.plurals.common_tokens_count, count = 1, formatArgs = wrappedList(1)),
        )
        assertThat(state.onClick).isNotNull()
        assertThat(row.tokens.map { it.id }).containsExactly(token.id.value)
        assertThat(row.tokens.single()).isInstanceOf(TangemTokenRow.State.Content::class.java)
    }

    @Test
    fun `GIVEN redesigned rows enabled AND searching WHEN convert THEN account row is not clickable`() {
        // Arrange
        val converter = createConverter(
            accounts = listOf(personalStatus()),
            isRedesignedTokenRowsEnabled = true,
            searchQuery = SearchQuery(token.name),
        )

        // Act
        val data = converter.convert() as TokenListUMData.AccountList

        // Assert
        val state = data.accountRows.values.single().state as TangemAccountRow.State.Content
        assertThat(state.onClick).isNull()
    }

    @Test
    fun `GIVEN redesigned rows enabled WHEN convert payment account THEN its row uses the Visa avatar`() {
        // Arrange
        val payment = MockAccounts.createPaymentAccountStatus(cryptoCurrency = token)
        val converter = createConverter(accounts = listOf(payment), isRedesignedTokenRowsEnabled = true)

        // Act
        val data = converter.convert() as TokenListUMData.AccountList

        // Assert
        val row = data.accountRows.getValue(data.tokensList.single().id)
        assertThat(row.avatar).isEqualTo(payment.account.toAvatarState())
        assertThat((row.state as TangemAccountRow.State.Content).label).isEqualTo(
            pluralReference(R.plurals.common_tokens_count, count = 1, formatArgs = wrappedList(1)),
        )
        assertThat(row.tokens.map { it.id }).containsExactly(token.id.value)
    }

    @Test
    fun `GIVEN redesigned rows enabled AND no accounts WHEN convert grouped list THEN section precedes token rows`() {
        // Arrange
        val personal = personalStatus()
        val grouped = TokenList.GroupedByNetwork(
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
            groups = listOf(
                TokenList.GroupedByNetwork.NetworkGroup(
                    network = token.network,
                    currencies = listOf(createCurrencyStatus(token)),
                ),
            ),
        )
        val converter = createConverter(
            params = TokenConverterParams.Wallet(personal, grouped),
            isRedesignedTokenRowsEnabled = true,
        )

        // Act
        val data = converter.convert() as TokenListUMData.TokenList

        // Assert
        assertThat(data.rows.map { it::class }).containsExactly(
            TokenRowItemUM.Section::class,
            TokenRowItemUM.Token::class,
        ).inOrder()
        assertThat((data.rows.first() as TokenRowItemUM.Section).title).isEqualTo(
            resourceReference(R.string.wallet_network_group_title, wrappedList(token.network.name)),
        )
    }

    @Test
    fun `GIVEN redesigned rows disabled AND no accounts WHEN convert THEN no token rows`() {
        // Arrange
        val personal = personalStatus()
        val converter = createConverter(
            params = TokenConverterParams.Wallet(personal, personal.tokenList),
            isRedesignedTokenRowsEnabled = false,
        )

        // Act
        val data = converter.convert() as TokenListUMData.TokenList

        // Assert
        assertThat(data.rows).isEmpty()
    }

    @Test
    fun `GIVEN redesigned rows disabled WHEN convert THEN no account rows`() {
        // Arrange
        val converter = createConverter(accounts = listOf(personalStatus()), isRedesignedTokenRowsEnabled = false)

        // Act
        val data = converter.convert() as TokenListUMData.AccountList

        // Assert
        assertThat(data.accountRows).isEmpty()
    }

    private fun personalStatus(): AccountStatus.Personal = AccountStatus.Personal(
        account = MockAccounts.createAccount(derivationIndex = 0, name = ACCOUNT_NAME, cryptoCurrencies = listOf(token)),
        tokenList = TokenList.Ungrouped(
            currencies = listOf(createCurrencyStatus(token)),
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
        ),
        priceChangeLce = Unit.lceError(),
    )

    private fun createCurrencyStatus(token: CryptoCurrency.Token): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = token,
        value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ONE,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(type = NetworkAddress.Address.Type.Primary, value = ADDRESS),
            ),
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private fun createConverter(
        accounts: List<AccountStatus>,
        isRedesignedTokenRowsEnabled: Boolean,
        searchQuery: SearchQuery = SearchQuery.Empty,
    ) = createConverter(
        params = TokenConverterParams.Account(
            accountList = AccountStatusList(
                userWalletId = MockAccounts.userWalletId,
                accountStatuses = accounts,
                totalAccounts = accounts.size,
                totalArchivedAccounts = 0,
                totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            ),
            expandedAccounts = accounts.mapTo(mutableSetOf()) { it.account.accountId },
        ),
        isRedesignedTokenRowsEnabled = isRedesignedTokenRowsEnabled,
        searchQuery = searchQuery,
    )

    private fun createConverter(
        params: TokenConverterParams,
        isRedesignedTokenRowsEnabled: Boolean,
        searchQuery: SearchQuery = SearchQuery.Empty,
    ) = ChooseTokenListItemConverter(
        appCurrency = AppCurrency.Default,
        params = params,
        config = ConverterConfig(
            clickIntents = mockk(relaxed = true),
            searchQuery = searchQuery,
            tokenFilter = { _, _ -> true },
            paymentAccountTokens = PaymentAccountTokens.IssuedTokens,
            balanceFilter = BalanceFilter.All,
            isJointAccountsShown = false,
            isRedesignedTokenRowsEnabled = isRedesignedTokenRowsEnabled,
        ),
    )

    private companion object {
        const val ACCOUNT_NAME = "Savings"
        const val ADDRESS = "0xaddress"
    }
}