package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import java.math.BigDecimal

internal fun cashbackRateTitle(cards: List<CashbackCard>): TextReference = when {
    cards.isEmpty() -> resourceReference(R.string.tangempay_cashback_title)
    cards.size == 1 -> resourceReference(
        id = R.string.tangempay_cashback_rate_title,
        formatArgs = wrappedList(cards.single().rate.formatRate()),
    )
    else -> resourceReference(
        id = R.string.tangempay_cashback_rate_title_up_to,
        formatArgs = wrappedList(cards.maxOf { it.rate }.formatRate()),
    )
}

internal fun BigDecimal.formatRate(): String = stripTrailingZeros().toPlainString()