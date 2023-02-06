package com.tangem.feature.referral.analytics

import com.tangem.core.analytics.AnalyticsEvent

sealed class ReferralEvents(
    event: String,
) : AnalyticsEvent(REFERRAL_PROGRAM_CATEGORY, event) {

    object ReferralScreenOpened : ReferralEvents(event = "Referral Screen Opened")
    object ClickParticipate : ReferralEvents(event = "Button - Participate")
    object ClickCopy : ReferralEvents(event = "Button - Copy")
    object ClickShare : ReferralEvents(event = "Button - Share")
    object ClickTaC : ReferralEvents(event = "Link - TaC")
}

private const val REFERRAL_PROGRAM_CATEGORY = "Referral program"
