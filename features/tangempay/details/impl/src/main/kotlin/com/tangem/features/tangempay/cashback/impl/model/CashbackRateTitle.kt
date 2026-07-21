package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList

internal fun cashbackRateTitle(rates: List<Int>): TextReference = when {
    rates.isEmpty() -> resourceReference(R.string.tangempay_cashback_title)
    rates.size == 1 -> resourceReference(
        id = R.string.tangempay_cashback_rate_title,
        formatArgs = wrappedList(rates.single().toString()),
    )
    else -> resourceReference(
        id = R.string.tangempay_cashback_rate_title_up_to,
        formatArgs = wrappedList(rates.max().toString()),
    )
}