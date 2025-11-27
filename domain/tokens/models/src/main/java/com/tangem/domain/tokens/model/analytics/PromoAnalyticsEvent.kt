package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class PromoAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Promotion", event = event, params = params) {
    class NoticePromotionBanner(
        source: AnalyticsParam.ScreensSources,
        program: Program,
    ) : PromoAnalyticsEvent(
        event = "Notice - Promotion Banner",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
            "Program Name" to program.programName,
        ),
    )

    class PromotionBannerClicked(
        source: AnalyticsParam.ScreensSources,
        program: Program,
        action: BannerAction,
    ) : PromoAnalyticsEvent(
        event = "Promo Banner Clicked",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
            "Program Name" to program.programName,
            "Action" to action.action,
        ),
    ) {
        sealed class BannerAction(val action: String) {
            data object Clicked : BannerAction(action = "Clicked")
            data object Closed : BannerAction(action = "Closed")
        }
    }

    // region visa waitlist promo
    data object VisaWaitlistPromo : PromoAnalyticsEvent(event = "Visa Waitlist")

    data object VisaWaitlistPromoJoin : PromoAnalyticsEvent(
        event = "Button - Join Now",
        params = mapOf(
            "Program Name" to "Visa Waitlist",
        ),
    )

    data object VisaWaitlistPromoDismiss : PromoAnalyticsEvent(
        event = "Button - Close",
        params = mapOf(
            "Program Name" to "Visa Waitlist",
        ),
    )
    //endregion

    // Use it on new promo action
    enum class Program(val programName: String) {
        Empty("Empty"),
        Sepa("Sepa"),
        BlackFriday("Black Friday"),
    }
}