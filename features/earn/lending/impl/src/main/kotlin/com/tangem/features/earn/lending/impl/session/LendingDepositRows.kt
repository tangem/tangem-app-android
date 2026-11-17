package com.tangem.features.earn.lending.impl.session

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.lending.impl.product.projectionOf
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.rate.formatEarnPercent
import java.math.BigDecimal
import javax.inject.Inject

internal class LendingDepositRows @Inject constructor(
    private val lendingProvider: LendingProvider,
) {

    /** @return nothing until the rate is known: the rows describe the rate */
    fun of(state: LendingAssetState, amount: BigDecimal): List<EarnRow> {
        val rate = state.market.rate?.fraction ?: return emptyList()

        return listOfNotNull(
            EarnRow(
                label = resourceReference(R.string.yield_module_earn_sheet_current_apy_title),
                value = EarnValue.Text(stringReference(rate.formatEarnPercent())),
            ),
            EarnRow(
                label = resourceReference(R.string.earn_product_details_performance_fee),
                value = EarnValue.Text(stringReference(lendingProvider.performanceFee.formatEarnPercent())),
            ),
            state.fiatRate?.let { fiatRate ->
                val yearlyEarnings = lendingProvider.projectionOf(rate).yearlyEstimate(amountFiat = amount * fiatRate)
                EarnRow(
                    label = resourceReference(R.string.lending_projected_earnings),
                    value = EarnValue.YearlyFiatEarnings(yearlyEarnings),
                )
            },
        )
    }
}