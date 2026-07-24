package com.tangem.features.foryou.impl.model

import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.TotalFiatBalance

/**
 * Aggregated portfolio across the accounts the user picked in the portfolio selector.
 *
 * The For You selector runs in [com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher.Mode.All]
 * (multi-choice over every wallet), so the selection can span several wallets. Each
 * [AccountCryptoCurrencyStatus] pairs a currency with its owning account (which knows its wallet), so
 * downstream converters can still route clicks to the right wallet after regrouping by asset/network.
 *
 * @property accountCryptoCurrencyStatuses the selected accounts' currency statuses (one entry per account × currency)
 * @property totalAccountsCount             total number of available accounts (before the selection filter), used to
 *                                          tell a full "all accounts" selection from a partial one
 * @property totalFiatBalance               aggregate fiat balance recomputed from the selected currencies
 *
 * Built by [com.tangem.features.foryou.impl.model.converter.ForYouSelectedPortfolioConverter].
 */
internal data class ForYouSelectedPortfolio(
    val accountCryptoCurrencyStatuses: List<AccountCryptoCurrencyStatus>,
    val totalAccountsCount: Int,
    val totalFiatBalance: TotalFiatBalance,
)