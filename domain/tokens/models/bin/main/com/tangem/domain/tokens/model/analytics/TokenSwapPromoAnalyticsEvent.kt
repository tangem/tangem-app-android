package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class TokenSwapPromoAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Promotion", event = event, params = params) {
    class NoticePromotionBanner(
        source: AnalyticsParam.ScreensSources,
        programName: ProgramName,
    ) : TokenSwapPromoAnalyticsEvent(
        event = "Notice - Promotion Banner",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
            "Program Name" to programName.name,
        ),
    )

    class PromotionBannerClicked(
        source: AnalyticsParam.ScreensSources,
        programName: ProgramName,
        action: BannerAction,
    ) : TokenSwapPromoAnalyticsEvent(
        event = "Promo Banner Clicked",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
            "Program Name" to programName.name,
            "Action" to action.action,
        ),
    ) {
        sealed class BannerAction(val action: String) {
            data object Clicked : BannerAction(action = "Clicked")
            data object Closed : BannerAction(action = "Closed")
        }
    }

    // Use it on new promo action
    enum class ProgramName {
        Empty,
    }
}