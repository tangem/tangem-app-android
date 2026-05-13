package com.tangem.features.staking.impl.presentation.state.utils

import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.utils.SECONDS_IN_HOUR
import com.tangem.domain.staking.model.CooldownPeriod
import com.tangem.domain.staking.model.Period
import com.tangem.features.staking.impl.R
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE

internal fun CooldownPeriod.toTextReference(): TextReference {
    return when (this) {
        is CooldownPeriod.Fixed -> when (period) {
            is Period.Days -> pluralReference(
                id = R.plurals.common_days,
                count = period.value,
                formatArgs = wrappedList(period.value),
            )
            is Period.Seconds -> {
                val hours = period.value / SECONDS_IN_HOUR
                pluralReference(
                    id = R.plurals.common_hours,
                    count = hours,
                    formatArgs = wrappedList(hours),
                )
            }
        }
        is CooldownPeriod.Range -> combinedReference(
            stringReference("$minDays$MINUS$maxDays$NON_BREAKING_SPACE"),
            pluralReference(
                id = R.plurals.common_days_no_param,
                count = maxDays,
            ),
        )
    }
}