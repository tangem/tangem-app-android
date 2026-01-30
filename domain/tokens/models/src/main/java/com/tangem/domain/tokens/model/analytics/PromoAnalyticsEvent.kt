package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class PromoAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Promotion", event = event, params = params) {
    data class NoticePromotionBanner(
        private val source: AnalyticsParam.ScreensSources,
        private val program: Program,
    ) : PromoAnalyticsEvent(
        event = "Notice - Promotion Banner",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
            "Program Name" to program.programName,
        ),
    )

    data class PromotionBannerClicked(
        private val source: AnalyticsParam.ScreensSources,
        private val program: Program,
        private val action: BannerAction,
    ) : PromoAnalyticsEvent(
        event = "Promo Banner Clicked",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
            "Program Name" to program.programName,
            "Action" to action.action,
        ),
    ) {
        sealed class BannerAction(val action: String) {
            class Clicked : BannerAction(action = "Clicked")
            class Closed : BannerAction(action = "Closed")
        }
    }

    // region visa waitlist promo
    class VisaWaitlistPromo : PromoAnalyticsEvent(event = "Visa Waitlist")

    class VisaWaitlistPromoJoin : PromoAnalyticsEvent(
        event = "Button - Join Now",
        params = mapOf(
            "Program Name" to "Visa Waitlist",
        ),
    )

    class VisaWaitlistPromoDismiss : PromoAnalyticsEvent(
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
        OnePlusOne("One-Plus-One"),
        YieldPromo("Yield Promo"),
    }
}