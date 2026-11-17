package com.tangem.features.earn.impl.banner.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.utils.StringsSigns

internal fun upToApyText(rate: String): TextReference =
    resourceReference(R.string.token_details_earn_staking_subtitle_apy, wrappedList(rate))

internal fun upToAprText(rate: String): TextReference =
    resourceReference(R.string.token_details_earn_staking_subtitle, wrappedList(rate))

internal fun activeCountText(amount: String?, count: Int): TextReference {
    val active = pluralReference(
        id = R.plurals.token_details_yield_active_count,
        count = count,
        formatArgs = wrappedList(count),
    )
    val separator = stringReference(" ${StringsSigns.MIDDLE_DOT} ")
    return if (amount == null) active else combinedReference(stringReference(amount), separator, active)
}