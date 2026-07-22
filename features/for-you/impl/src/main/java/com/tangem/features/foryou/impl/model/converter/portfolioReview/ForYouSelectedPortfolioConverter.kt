package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero

/**
 * Builds the [ForYouSelectedPortfolio] from every wallet's accounts, keeping only the accounts the user picked
 * in the portfolio selector ([selectedAccounts]).
 *
 * [ForYouSelectedPortfolio.totalAccountsCount] keeps the total number of available crypto-portfolio accounts
 * across all wallets (before filtering) so the selector badge can tell "all accounts" from a partial selection.
 * [ForYouSelectedPortfolio.totalFiatBalance] is recomputed from the selected currencies (not summed from the
 * per-wallet [AccountStatusList.totalFiatBalance]) so the state reflects the actual selection: any still-loading
 * currency keeps the total [TotalFiatBalance.Loading], an empty selection is [TotalFiatBalance.Failed], otherwise
 * the amounts are summed and the most conservative source across the selection is reported.
 */
internal class ForYouSelectedPortfolioConverter(
    private val selectedAccounts: Set<AccountId>,
) : Converter<Map<UserWalletId, AccountStatusList>, ForYouSelectedPortfolio> {

    override fun convert(value: Map<UserWalletId, AccountStatusList>): ForYouSelectedPortfolio {
        val allAccounts = value.values
            .flatMap { it.accountStatuses }
            .filterCryptoPortfolio()

        val accountCryptoCurrencyStatus = allAccounts
            .filter { it.accountId in selectedAccounts }
            .flatMap { accountStatus ->
                accountStatus.flattenCurrencies().map { status ->
                    AccountCryptoCurrencyStatus(account = accountStatus.account, status = status)
                }
            }

        return ForYouSelectedPortfolio(
            accountCryptoCurrencyStatuses = accountCryptoCurrencyStatus,
            totalAccountsCount = allAccounts.size,
            totalFiatBalance = accountCryptoCurrencyStatus.toTotalFiatBalance(),
        )
    }

    private fun List<AccountCryptoCurrencyStatus>.toTotalFiatBalance(): TotalFiatBalance = when {
        isEmpty() -> TotalFiatBalance.Failed
        all { it.status.value is CryptoCurrencyStatus.Loading } -> TotalFiatBalance.Loading
        else -> TotalFiatBalance.Loaded(
            amount = sumOf { it.status.value.fiatAmount.orZero() },
            source = map { it.status.value.sources.total }.worstSource(),
        )
    }

    /**
     * The most conservative source across the selection: any [StatusSource.ONLY_CACHE] (could-not-refresh)
     * dominates a [StatusSource.CACHE] (still refreshing), which dominates [StatusSource.ACTUAL].
     */
    private fun List<StatusSource>.worstSource(): StatusSource = when {
        any { it == StatusSource.ONLY_CACHE } -> StatusSource.ONLY_CACHE
        any { it == StatusSource.CACHE } -> StatusSource.CACHE
        else -> StatusSource.ACTUAL
    }
}