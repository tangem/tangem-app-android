package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.EventValue

sealed class TokenSwapPromoAnalyticsEvent(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent(category = "Promotion", event = event, params = params) {

    class NoticePromotionBanner(
        source: AnalyticsParam.ScreensSources,
        programName: ProgramName,
    ) : TokenSwapPromoAnalyticsEvent(
        event = "Notice - Promotion Banner",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value.asStringValue(),
            "Program Name" to programName.name.asStringValue(),
        ),
    )

    class PromotionBannerClicked(
        source: AnalyticsParam.ScreensSources,
        programName: ProgramName,
        action: BannerAction,
    ) : TokenSwapPromoAnalyticsEvent(
        event = "Promo Banner Clicked",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value.asStringValue(),
            "Program Name" to programName.name.asStringValue(),
            "Action" to action.action.asStringValue(),
        ),
    ) {
        sealed class BannerAction(val action: String) {
            data object Clicked : BannerAction(action = "Clicked")
            data object Closed : BannerAction(action = "Closed")
        }
    }

    enum class ProgramName {
        OKX,
        Ring,
    }
}