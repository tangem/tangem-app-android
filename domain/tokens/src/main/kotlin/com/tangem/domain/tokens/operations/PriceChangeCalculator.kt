package com.tangem.domain.tokens.operations

import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Utility object for calculating the [PriceChange] of a cryptocurrency portfolio.
 *
 * This object provides methods to calculate the weighted average price change of a list of cryptocurrency statuses
 * based on their fiat balance weights.
 */
object PriceChangeCalculator {

    /**
     * Calculates the weighted average price change of a list of cryptocurrency statuses.
     *
     * @param statuses A list of `CryptoCurrencyStatus` objects representing the statuses of cryptocurrencies.
     * @return An `Lce` object containing the calculated `PriceChange` or an error/loading state.
     *         - Returns `Lce.Content` with the calculated `PriceChange` if successful.
     *         - Returns `Lce.Loading` if the total fiat balance is still loading.
     *         - Returns `Lce.Error` if the total fiat balance calculation fails.
     */
    fun calculate(statuses: List<CryptoCurrencyStatus>): Lce<Unit, PriceChange> {
        val walletTotalFiatBalance = statuses.toNonEmptyListOrNull()
            ?.let(TotalFiatBalanceCalculator::calculate)
            ?: TokenList.Empty.totalFiatBalance

        val balance = when (walletTotalFiatBalance) {
            is TotalFiatBalance.Loaded -> walletTotalFiatBalance.amount
            TotalFiatBalance.Loading -> {
                return lceLoading()
            }
            TotalFiatBalance.Failed -> {
                return Lce.Error(error = Unit)
            }
        }

        if (balance.isZero()) {
            return createZeroPriceChange(source = walletTotalFiatBalance.source).lceContent()
        }

        val total = statuses.sumOf {
            val weight = it.value.fiatAmount.orZero().divide(balance, 2, RoundingMode.HALF_UP)
            val priceChange = it.value.priceChange.orZero()

            weight * priceChange
        }.stripTrailingZeros()

        return PriceChange(value = total, source = walletTotalFiatBalance.source).lceContent()
    }

    private fun createZeroPriceChange(source: StatusSource): PriceChange {
        return PriceChange(
            value = BigDecimal.ZERO.movePointLeft(2),
            source = source,
        )
    }
}