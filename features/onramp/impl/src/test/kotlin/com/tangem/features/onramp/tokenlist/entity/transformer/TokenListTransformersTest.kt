package com.tangem.features.onramp.tokenlist.entity.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.tokenlist.entity.*
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class TokenListTransformersTest {
    private val currency = MockCryptoCurrencyFactory().ethereum
    private val status = CryptoCurrencyStatus(currency, CryptoCurrencyStatus.Loading)
    private val account = MockAccounts.createAccount(1, cryptoCurrencies = listOf(currency))
    private val accountStatus = mockk<AccountStatus.Personal> {
        every { this@mockk.account } returns this@TokenListTransformersTest.account
        every { tokenList.flattenCurrencies() } returns listOf(status)
    }
    private val initial = TokenListUMController().state.value
    private val message = stringReference("No matching tokens")

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN populated account WHEN loading THEN mode and token count are preserved`(accountsMode: Boolean) {
        // Arrange
        val transformer = SetLoadingAccountTokenListTransformer(AppCurrency.Default, listOf(accountStatus), accountsMode)

        // Act
        val state = transformer.transform(initial)

        // Assert
        assertThat(state.tokensListData.totalTokensCount).isEqualTo(1)
        assertThat(state.searchBarUM).isEqualTo(initial.searchBarUM)
        if (accountsMode) {
            assertThat((state.tokensListData as TokenListUMData.AccountList).tokensList).hasSize(1)
        } else {
            val token = (state.tokensListData as TokenListUMData.TokenList).tokensList.single()
            assertThat(token).isEqualTo(LoadingTokenListItemConverter.convert(currency))
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN unavailable account token WHEN updated THEN correct mode retains count and hides balance`(
        accountsMode: Boolean,
    ) {
        // Arrange
        val transformer = UpdateAccountTokenListTransformer(
            AppCurrency.Default, { _, _ -> },
            listOf(AccountAvailabilityUM(account, listOf(AccountCurrencyUM(false, status)))),
            true, message, isAccountsMode = accountsMode,
        )

        // Act
        val state = transformer.transform(initial)

        // Assert
        assertThat(state.tokensListData.totalTokensCount).isEqualTo(1)
        assertThat(state.isBalanceHidden).isTrue()
        assertThat(state.searchBarUM).isEqualTo(initial.searchBarUM)
        if (accountsMode) {
            assertThat((state.tokensListData as TokenListUMData.AccountList).tokensList).hasSize(1)
        } else {
            val tokens = (state.tokensListData as TokenListUMData.TokenList).tokensList
            assertThat(tokens).hasSize(2)
            assertThat(tokens.first()).isInstanceOf(TokensListItemUM.GroupTitle::class.java)
        }
    }

    @Test
    fun `GIVEN existing tokens WHEN search has no matches THEN stale lists are cleared`() {
        // Arrange
        val stale = initial.copy(
            availableItems = persistentListOf(LoadingTokenListItemConverter.convert(currency)),
            unavailableItems = persistentListOf(LoadingTokenListItemConverter.convert(currency)),
        )

        // Act
        val state = SetNothingToFoundStateTransformer(true, message).transform(stale)

        // Assert
        assertThat(state).isEqualTo(
            stale.copy(
                availableItems = persistentListOf(),
                unavailableItems = persistentListOf(),
                isBalanceHidden = true,
                tokensListData = TokenListUMData.TokenList(
                    persistentListOf(TokensListItemUM.Text(message.hashCode(), message)), 0,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN no statuses WHEN legacy items update THEN stale lists and warning are cleared`() {
        // Arrange
        val stale = initial.copy(
            availableItems = persistentListOf(LoadingTokenListItemConverter.convert(currency)),
            unavailableItems = persistentListOf(LoadingTokenListItemConverter.convert(currency)),
        )

        // Act
        val state = UpdateTokenItemsTransformer(
            AppCurrency.Default, { _, _ -> }, emptyMap(), true, message,
        ).transform(stale)

        // Assert
        assertThat(state).isEqualTo(
            stale.copy(availableItems = persistentListOf(), unavailableItems = persistentListOf(), isBalanceHidden = true),
        )
    }
}