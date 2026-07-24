package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackInfoTilesUM

internal class TangemPayCashbackInfoTilesConverter(
    private val onRateClick: () -> Unit,
    private val onAccrualsClick: () -> Unit,
) {

    fun convert(tiers: List<CashbackTier>, currentPlan: TangemPayTariffPlan?): TangemPayCashbackInfoTilesUM {
        return TangemPayCashbackInfoTilesUM(
            rate = TangemPayCashbackInfoTilesUM.Tile(
                iconRes = R.drawable.ic_percent_24,
                title = rateTitle(tiers = tiers, currentPlan = currentPlan),
                subtitle = currentPlan?.name
                    ?.let { resourceReference(R.string.tangempay_cashback_rate_subtitle, wrappedList(it)) }
                    ?: TextReference.EMPTY,
                onClick = onRateClick,
            ),
            accruals = TangemPayCashbackInfoTilesUM.Tile(
                iconRes = R.drawable.ic_information_24,
                title = resourceReference(R.string.tangempay_cashback_accruals_title),
                subtitle = resourceReference(R.string.tangempay_cashback_accruals_subtitle),
                onClick = onAccrualsClick,
            ),
        )
    }

    private fun rateTitle(tiers: List<CashbackTier>, currentPlan: TangemPayTariffPlan?): TextReference {
        val rates = tiers.mapNotNull { it.rate }
        return when {
            rates.isEmpty() -> resourceReference(R.string.tangempay_cashback_title)
            currentPlan != null && !currentPlan.isBasicTier -> resourceReference(
                id = R.string.tangempay_cashback_rate_title_up_to,
                formatArgs = wrappedList(rates.max().toString()),
            )
            else -> resourceReference(
                id = R.string.tangempay_cashback_rate_title,
                formatArgs = wrappedList(rates.min().toString()),
            )
        }
    }
}