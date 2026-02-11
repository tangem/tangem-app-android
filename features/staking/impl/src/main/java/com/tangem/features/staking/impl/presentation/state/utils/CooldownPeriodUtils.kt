package com.tangem.features.staking.impl.presentation.state.utils

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.staking.model.CooldownPeriod
import com.tangem.features.staking.impl.R
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE

internal fun CooldownPeriod.toTextReference(): TextReference {
    return when (this) {
        is CooldownPeriod.Fixed -> pluralReference(
            id = R.plurals.common_days,
            count = days,
            formatArgs = wrappedList(days),
        )
        is CooldownPeriod.Range -> combinedReference(
            stringReference("$minDays$MINUS$maxDays$NON_BREAKING_SPACE"),
            pluralReference(
                id = R.plurals.common_days_no_param,
                count = maxDays,
            ),
        )
    }
}