package com.tangem.features.earn.impl.analytics

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.Withdraw
import java.util.Locale

internal sealed class EarnInfoAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String>,
) : EarnAnalyticsEvent(category = category, event = event, params = params) {

    class InfoScreenOpened(
        category: String,
        source: EarnEntrySource,
        positionBucket: String?,
        earnedBucket: String?,
        currentApy: String?,
    ) : EarnInfoAnalyticsEvent(
        category = category,
        event = "Info Screen Opened",
        params = buildMap {
            put(AnalyticsParam.SOURCE, source.toAnalyticsValue())
            positionBucket?.let { put(EarnAnalyticsParam.POSITION_BUCKET, it) }
            earnedBucket?.let { put(EarnAnalyticsParam.EARNED_BUCKET, it) }
            currentApy?.let { put(EarnAnalyticsParam.CURRENT_APY, it) }
        },
    )

    class ChartPeriod(
        category: String,
        period: String,
    ) : EarnInfoAnalyticsEvent(
        category = category,
        event = "Button - Chart Period",
        params = mapOf(EarnAnalyticsParam.PERIOD to period),
    )

    class InfoScreenAction(
        category: String,
        action: InfoAction,
    ) : EarnInfoAnalyticsEvent(
        category = category,
        event = "Button - Info Screen Action",
        params = mapOf(EarnAnalyticsParam.ACTION to action.value),
    )
}

internal enum class InfoAction(val value: String) {
    Withdraw("Withdraw"),
    AddFunds("Add Funds"),
}

internal fun EarnAction.toInfoAction(): InfoAction? = when (this) {
    is Withdraw -> InfoAction.Withdraw
    is Deposit -> InfoAction.AddFunds
    else -> null
}

internal fun EarnPeriod.toAnalyticsValue(): String? = id.uppercase(Locale.ROOT).takeIf { it in ANALYTICS_PERIODS }

private val ANALYTICS_PERIODS = setOf("1M", "3M", "6M")