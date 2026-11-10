package com.tangem.features.earn.impl.overview

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.formatEarnRate
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class EarnRateBadgeConverter @Inject constructor() : Converter<EarnProductRate, TextReference> {

    override fun convert(value: EarnProductRate): TextReference {
        val badgeRes = when (value.type) {
            EarnRateType.APR -> R.string.staking_apr_earn_badge
            EarnRateType.APY -> R.string.yield_module_earn_badge
        }
        return resourceReference(badgeRes, wrappedList(value.fraction.formatEarnRate()))
    }
}