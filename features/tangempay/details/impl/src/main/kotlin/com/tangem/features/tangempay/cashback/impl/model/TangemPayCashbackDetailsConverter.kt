package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackDetailsUM
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayCashbackDetailsConverter {

    fun convert(
        tiers: List<CashbackTier>,
        payoutCurrency: String?,
        monthlyCap: CashbackPromotions.MonthlyCap?,
    ): TangemPayCashbackDetailsUM {
        val rows = buildList {
            tiers.forEach { add(tierRow(it)) }
            if (tiers.isNotEmpty()) {
                add(resourceReference(R.string.tangempay_cashback_details_eu_excluded))
                if (!payoutCurrency.isNullOrEmpty()) {
                    add(resourceReference(R.string.tangempay_cashback_details_paid_in, wrappedList(payoutCurrency)))
                }
                if (monthlyCap != null) {
                    add(resourceReference(R.string.tangempay_cashback_details_cap, wrappedList(monthlyCap.formatted())))
                }
            }
        }
        return TangemPayCashbackDetailsUM(
            title = cashbackRateTitle(tiers.mapNotNull { it.rate }),
            rows = rows.toImmutableList(),
        )
    }

    private fun tierRow(tier: CashbackTier): TextReference = resourceReference(
        id = R.string.tangempay_cashback_details_tier,
        formatArgs = wrappedList(tier.rate?.toString().orEmpty(), tier.label, tier.minPurchase.orEmpty()),
    )

    private fun CashbackPromotions.MonthlyCap.formatted(): String {
        val javaCurrency = getJavaCurrencyByCode(currency ?: DEFAULT_CURRENCY_CODE)
        return amount.format { fiat(javaCurrency.currencyCode, javaCurrency.symbol).optionalDecimals() }
    }

    private companion object {
        const val DEFAULT_CURRENCY_CODE = "USD"
    }
}