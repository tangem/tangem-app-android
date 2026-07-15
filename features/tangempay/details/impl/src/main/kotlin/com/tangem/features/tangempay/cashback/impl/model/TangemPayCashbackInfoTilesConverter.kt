package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackInfoTilesUM

internal class TangemPayCashbackInfoTilesConverter(
    private val onRateClick: () -> Unit,
    private val onAccrualsClick: () -> Unit,
) {

    // TODO([REDACTED_TASK_KEY]): move hardcoded strings to string resources
    fun convert(tiers: List<CashbackTier>, currentPlan: TangemPayTariffPlan?): TangemPayCashbackInfoTilesUM {
        val rate = tiers.selectTier(currentPlan?.tierId)?.rate
        return TangemPayCashbackInfoTilesUM(
            rate = TangemPayCashbackInfoTilesUM.Tile(
                iconRes = R.drawable.ic_percent_24,
                title = stringReference(if (rate != null) "Cashback $rate%" else "Cashback"),
                subtitle = currentPlan?.name?.let { stringReference("With your $it plan") } ?: TextReference.EMPTY,
                onClick = onRateClick,
            ),
            accruals = TangemPayCashbackInfoTilesUM.Tile(
                iconRes = R.drawable.ic_information_24,
                title = stringReference("Accruals"),
                subtitle = stringReference("Limits and exceptions"),
                onClick = onAccrualsClick,
            ),
        )
    }

    private fun List<CashbackTier>.selectTier(tierId: String?): CashbackTier? {
        return firstOrNull { it.tierId == tierId } ?: firstOrNull()
    }
}