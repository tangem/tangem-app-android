package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackInfoTilesUM

internal class TangemPayCashbackInfoTilesConverter(
    private val onRateClick: () -> Unit,
    private val onAccrualsClick: () -> Unit,
) {

    fun convert(cards: List<CashbackCard>): TangemPayCashbackInfoTilesUM {
        return TangemPayCashbackInfoTilesUM(
            rate = TangemPayCashbackInfoTilesUM.Tile(
                iconRes = R.drawable.ic_percent_24,
                title = cashbackRateTitle(cards),
                subtitle = cards.subtitle(),
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

    private fun List<CashbackCard>.subtitle(): TextReference {
        val titles = mapNotNull { it.title?.takeIf(String::isNotBlank) }
        return if (titles.isEmpty()) TextReference.EMPTY else stringReference(titles.joinToString())
    }
}