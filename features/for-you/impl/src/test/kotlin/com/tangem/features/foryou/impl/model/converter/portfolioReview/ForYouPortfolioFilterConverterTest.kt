package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createEarnCurrency
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createPortfolioStatus
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ForYouPortfolioFilterConverterTest {

    // Held as fields so the expected chips can reference the very same lambda instances the converter
    // is given — that way a whole-object isEqualTo also proves the callbacks landed in the right slots.
    private val onClick: () -> Unit = {}
    private val onClearClick: () -> Unit = {}

    private val converter = ForYouPortfolioFilterConverter(onClick = onClick, onClearClick = onClearClick)

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = converter.convert(model.portfolio)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<ConvertModel> {
        val account1 = MockAccounts.createAccount(derivationIndex = 1)
        val account2 = MockAccounts.createAccount(derivationIndex = 2)

        return listOf(
            // Every available account picked — the portfolio is not filtered.
            ConvertModel(
                portfolio = createSelectedPortfolio(
                    accountStatusWithCurrency(account1),
                    accountStatusWithCurrency(account2),
                    totalAccountsCount = 2,
                ),
                expected = allAccountsChip(),
            ),
            // Nothing picked yet — also treated as unfiltered.
            ConvertModel(
                portfolio = createSelectedPortfolio(totalAccountsCount = 1),
                expected = allAccountsChip(),
            ),
            // A single account out of many — the chip names it.
            ConvertModel(
                portfolio = createSelectedPortfolio(
                    accountStatusWithCurrency(account1),
                    totalAccountsCount = 2,
                ),
                expected = TangemFilterItemUM.Active(
                    id = ForYouPortfolioFilterConverter.ID,
                    value = account1.accountName.toUM().value,
                    counter = null,
                    onClick = onClick,
                    onClearClick = onClearClick,
                ),
            ),
            // Several accounts out of many — the generic label plus the picked count.
            ConvertModel(
                portfolio = createSelectedPortfolio(
                    accountStatusWithCurrency(account1),
                    accountStatusWithCurrency(account2),
                    totalAccountsCount = 3,
                ),
                expected = TangemFilterItemUM.Active(
                    id = ForYouPortfolioFilterConverter.ID,
                    value = resourceReference(R.string.common_accounts),
                    counter = 2,
                    onClick = onClick,
                    onClearClick = onClearClick,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN several currencies of one account WHEN convert THEN the account is counted once`() {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1)
        val portfolio = createSelectedPortfolio(
            createPortfolioStatus(
                currencies = listOf(
                    createStatus(createEarnCurrency(currencyId = "coin-ethereum")),
                    createStatus(createEarnCurrency(currencyId = "coin-bitcoin")),
                ),
                account = account,
            ),
            totalAccountsCount = 2,
        )

        // Act
        val actual = converter.convert(portfolio)

        // Assert
        assertThat(actual).isEqualTo(
            TangemFilterItemUM.Active(
                id = ForYouPortfolioFilterConverter.ID,
                value = account.accountName.toUM().value,
                counter = null,
                onClick = onClick,
                onClearClick = onClearClick,
            ),
        )
    }

    private fun allAccountsChip() = TangemFilterItemUM.Inactive(
        id = ForYouPortfolioFilterConverter.ID,
        label = resourceReference(R.string.common_all_accounts),
        onClick = onClick,
    )

    /** A picked account carrying a single currency, so it shows up in the portfolio's account statuses. */
    private fun accountStatusWithCurrency(account: Account.CryptoPortfolio): AccountStatus.CryptoPortfolio =
        createPortfolioStatus(
            currencies = listOf(createStatus(createEarnCurrency())),
            account = account,
        )

    internal data class ConvertModel(
        val portfolio: ForYouSelectedPortfolio,
        val expected: TangemFilterItemUM,
    )
}