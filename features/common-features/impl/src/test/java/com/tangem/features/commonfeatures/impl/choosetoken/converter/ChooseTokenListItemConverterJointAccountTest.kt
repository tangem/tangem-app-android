package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.PaymentAccountTokens
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter
import com.tangem.features.commonfeatures.api.choosetoken.model.TokenListUMData
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * A joint account enters the chooser only for the flows that never spend from the wallet's own keys, and
 * only when it is active — a pending account has no Safe address to receive funds to yet.
 */
internal class ChooseTokenListItemConverterJointAccountTest {

    private val token = MockAccounts.createPaymentAccountToken()

    @Test
    fun `GIVEN joint accounts opted in WHEN convert THEN the active account section lists its tokens`() {
        // Arrange
        val converter = createConverter(
            jointStatus = jointStatus(value = activeValue()),
            isJointAccountsShown = true,
        )

        // Act
        val data = converter.convert()

        // Assert
        val portfolio = (data as TokenListUMData.AccountList).tokensList
            .filterIsInstance<TokensListItemUM.Portfolio>()
            .single()
        assertThat(portfolio.tokensItemsList.map { it.state.id }).containsExactly(token.id.value)
    }

    @Test
    fun `GIVEN joint accounts not opted in WHEN convert THEN the active account is hidden`() {
        // Arrange
        val converter = createConverter(
            jointStatus = jointStatus(value = activeValue()),
            isJointAccountsShown = false,
        )

        // Act
        val data = converter.convert()

        // Assert
        assertThat(data).isInstanceOf(TokenListUMData.EmptyList::class.java)
    }

    @Test
    fun `GIVEN pending joint account WHEN convert THEN it is hidden even when opted in`() {
        // Arrange
        val converter = createConverter(
            jointStatus = jointStatus(
                value = JointAccountStatusValue.Pending(StatusSource.ACTUAL, joinedMembers = 1, totalMembers = 3),
                tokenList = TokenList.Empty,
            ),
            isJointAccountsShown = true,
        )

        // Act
        val data = converter.convert()

        // Assert
        assertThat(data).isInstanceOf(TokenListUMData.EmptyList::class.java)
    }

    private fun activeValue(): JointAccountStatusValue.Active =
        JointAccountStatusValue.Active(StatusSource.ACTUAL, totalMembers = 3, safeAddress = SAFE)

    private fun jointStatus(
        value: JointAccountStatusValue,
        tokenList: TokenList = ungroupedTokenList(),
    ): AccountStatus.Joint = AccountStatus.Joint(
        account = MockAccounts.createJointAccount(cryptoCurrencies = listOf(token)),
        value = value,
        tokenList = tokenList,
        priceChangeLce = Unit.lceError(),
    )

    private fun ungroupedTokenList(): TokenList.Ungrouped = TokenList.Ungrouped(
        currencies = listOf(createCurrencyStatus(token)),
        totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL),
        sortedBy = TokensSortType.NONE,
    )

    private fun createCurrencyStatus(token: CryptoCurrency.Token): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = token,
        value = CryptoCurrencyStatus.NoQuote(
            amount = BigDecimal.ONE,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    type = NetworkAddress.Address.Type.Primary,
                    value = SAFE,
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
        jointStatus: AccountStatus.Joint,
        isJointAccountsShown: Boolean,
    ) = ChooseTokenListItemConverter(
        appCurrency = AppCurrency.Default,
        params = TokenConverterParams.Account(
            accountList = createAccountList(jointStatus),
            expandedAccounts = setOf(jointStatus.account.accountId),
        ),
        config = ConverterConfig(
            clickIntents = mockk(relaxed = true),
            searchQuery = SearchQuery.Empty,
            tokenFilter = { _, _ -> true },
            paymentAccountTokens = PaymentAccountTokens.Hidden,
            balanceFilter = BalanceFilter.All,
            isJointAccountsShown = isJointAccountsShown,
        ),
    )

    private fun createAccountList(jointStatus: AccountStatus.Joint) = AccountStatusList(
        userWalletId = MockAccounts.userWalletId,
        accountStatuses = listOf(jointStatus),
        totalAccounts = 1,
        totalArchivedAccounts = 0,
        totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
        sortType = TokensSortType.NONE,
        groupType = TokensGroupType.NONE,
    )

    private companion object {
        const val SAFE = "0x5afe000000000000000000000000000000000001"
    }
}