package com.tangem.feature.referral.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class ReferralEvents(event: String) : AnalyticsEvent(REFERRAL_PROGRAM_CATEGORY, event) {

    class ReferralScreenOpened : ReferralEvents(event = "Referral Screen Opened")
    class ClickParticipate : ReferralEvents(event = "Button - Participate")
    class ClickCopy : ReferralEvents(event = "Button - Copy")
    class ClickShare : ReferralEvents(event = "Button - Share")
    class ClickTaC : ReferralEvents(event = "Link - TaC")
    class ParticipateSuccessful : ReferralEvents(event = "Participate Successful")
}

sealed class ReferralEventsAccounts(event: String) : AnalyticsEvent(REFERRAL_PROGRAM_ACCOUNT_CATEGORY, event) {

    class ListChooseAccount : ReferralEventsAccounts(event = "List - Choose Account")
}

private const val REFERRAL_PROGRAM_CATEGORY = "Referral Program"
private const val REFERRAL_PROGRAM_ACCOUNT_CATEGORY = "Referral program / Account"