package com.tangem.features.feed.earn.ui.state

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

internal enum class EarnFilterTypeUM(val title: TextReference, val text: TextReference) {
    All(
        title = resourceReference(R.string.earn_filter_all_types),
        text = resourceReference(R.string.earn_filter_all_types_text),
    ),
    Staking(
        title = resourceReference(R.string.common_staking),
        text = resourceReference(R.string.earn_filter_staking_type_text),
    ),
    YieldMode(
        title = resourceReference(R.string.markets_sort_by_yield_mode_title),
        text = resourceReference(R.string.earn_filter_yield_type_text),
    ),
}