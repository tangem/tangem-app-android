package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackInfoTilesUM

internal class TangemPayCashbackInfoTilesConverter(
    private val onRateClick: () -> Unit,
    private val onAccrualsClick: () -> Unit,
) {

    fun convert(cards: List<CashbackCard>): TangemPayCashbackInfoTilesUM {
        return TangemPayCashbackInfoTilesUM(
            rate = TangemPayCashbackInfoTilesUM.Tile(
                icon = Icons.ic_percent_backward_20,
                title = cashbackRateTitle(cards),
                subtitle = cards.subtitle(),
                onClick = onRateClick,
            ),
            accruals = TangemPayCashbackInfoTilesUM.Tile(
                icon = Icons.ic_info_20,
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