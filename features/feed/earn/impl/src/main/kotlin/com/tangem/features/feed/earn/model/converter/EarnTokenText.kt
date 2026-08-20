package com.tangem.features.feed.earn.model.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnType
import com.tangem.features.feed.earn.impl.R

internal fun EarnToken.earnValueText(): TextReference {
    val apyText = stringReference(apy.parseBigDecimalOrNull().format { percent(withPercentSign = false) })

    return when (rewardType) {
        EarnRewardType.APR -> styledResourceReference(
            id = R.string.staking_apr_earn_badge,
            formatArgs = wrappedList(apyText),
            spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.status.success) },
        )
        EarnRewardType.APY -> styledResourceReference(
            id = R.string.yield_module_earn_badge,
            formatArgs = wrappedList(apyText),
            spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.status.success) },
        )
    }
}

internal fun EarnType.toTitleText(): TextReference = when (this) {
    EarnType.STAKING -> TextReference.Res(R.string.common_staking)
    EarnType.YIELD -> TextReference.Res(R.string.common_yield_mode)
}