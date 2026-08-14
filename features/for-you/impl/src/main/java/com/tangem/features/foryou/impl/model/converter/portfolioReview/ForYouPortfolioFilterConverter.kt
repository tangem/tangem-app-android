package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.utils.converter.Converter

/**
 * Builds the portfolio-selector filter chip shown next to the "Portfolio review" title from the
 * accounts the user picked in the selector.
 *
 * The chip state encodes whether the portfolio is filtered at all:
 * - nothing picked, or every available account picked — the portfolio is unfiltered, so the chip is
 *   [TangemFilterItemUM.Inactive] and only invites the user to narrow the selection down;
 * - exactly one account — [TangemFilterItemUM.Active] showing that account's name, so the user sees
 *   *which* account they are looking at;
 * - several accounts — [TangemFilterItemUM.Active] showing the generic "Accounts" label with the
 *   number of picked accounts as the counter, since no single name would describe the selection.
 *
 * "Every available account picked" is decided against [ForYouSelectedPortfolio.totalAccountsCount],
 * which counts the crypto-portfolio accounts across all wallets before the selection filter.
 *
 * @property onClick invoked when the chip is tapped — opens the portfolio selector
 * @property onClearClick invoked when the trailing cross of an active chip is tapped — resets the
 * selection back to all accounts
 */
internal class ForYouPortfolioFilterConverter(
    private val onClick: () -> Unit,
    private val onClearClick: () -> Unit,
) : Converter<ForYouSelectedPortfolio, TangemFilterItemUM> {

    override fun convert(value: ForYouSelectedPortfolio): TangemFilterItemUM {
        val selectedAccounts = value.accountCryptoCurrencyStatuses
            .map { it.account }
            .distinct()

        return when {
            selectedAccounts.isEmpty() || selectedAccounts.size == value.totalAccountsCount -> {
                TangemFilterItemUM.Inactive(
                    id = ID,
                    label = resourceReference(R.string.common_all_accounts),
                    onClick = onClick,
                )
            }
            selectedAccounts.size == 1 -> TangemFilterItemUM.Active(
                id = ID,
                value = selectedAccounts.first().accountName.toUM().value,
                onClick = onClick,
                onClearClick = onClearClick,
            )
            else -> TangemFilterItemUM.Active(
                id = ID,
                value = resourceReference(R.string.common_accounts),
                counter = selectedAccounts.size,
                onClick = onClick,
                onClearClick = onClearClick,
            )
        }
    }

    companion object {

        /**
         * Stable chip identity, shared with the initial [TangemFilterItemUM.Loading] built in
         * `ForYouModel`. The screen shows a single filter, and keeping one id across all states lets
         * the chip keep its press state while the selection changes.
         */
        const val ID = "portfolio_selector"
    }
}