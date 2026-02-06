package com.tangem.features.feed.ui.earn.state

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

internal enum class EarnFilterTypeUM(val text: TextReference) {
    All(resourceReference(R.string.earn_filter_all_types)),
    Staking(resourceReference(R.string.common_staking)),
    YieldMode(resourceReference(R.string.markets_sort_by_yield_mode_title)),
}