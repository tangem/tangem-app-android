package com.tangem.feature.referral.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class ReferralEvents(event: String) : AnalyticsEvent(REFERRAL_PROGRAM_CATEGORY, event) {

    data object ReferralScreenOpened : ReferralEvents(event = "Referral Screen Opened")
    data object ClickParticipate : ReferralEvents(event = "Button - Participate")
    data object ClickCopy : ReferralEvents(event = "Button - Copy")
    data object ClickShare : ReferralEvents(event = "Button - Share")
    data object ClickTaC : ReferralEvents(event = "Link - TaC")
    data object ParticipateSuccessful : ReferralEvents(event = "Participate Successful")
}

private const val REFERRAL_PROGRAM_CATEGORY = "Referral Program"