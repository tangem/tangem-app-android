package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackDetailsUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayCashbackDetailsConverter : Converter<List<CashbackTier>, TangemPayCashbackDetailsUM> {

    // TODO([REDACTED_TASK_KEY]): move hardcoded strings to string resources
    override fun convert(value: List<CashbackTier>): TangemPayCashbackDetailsUM {
        return TangemPayCashbackDetailsUM(
            title = title(value),
            rows = value.map(::tierRow).toImmutableList(),
        )
    }

    private fun title(tiers: List<CashbackTier>): TextReference {
        val rates = tiers.mapNotNull { it.rate }
        return when {
            rates.isEmpty() -> stringReference("Cashback")
            rates.size == 1 -> stringReference("Cashback ${rates.single()}%")
            else -> stringReference("Cashback up to ${rates.max()}%")
        }
    }

    private fun tierRow(tier: CashbackTier): TextReference {
        val rate = tier.rate?.let { "$it% for " }.orEmpty()
        val min = tier.minPurchase?.let { ", min purchase $it" }.orEmpty()
        val cap = tier.monthlyCap?.let { ", up to $it per month" }.orEmpty()
        return stringReference("$rate${tier.scope} with your ${tier.label}$min$cap")
    }
}