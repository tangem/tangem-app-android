package com.tangem.feature.learn2earn.analytics

import com.tangem.core.analytics.AnalyticsEvent

internal sealed class Learn2earnEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(
    category = category,
    event = event,
    params = params + mapOf(
        "Program Name" to "1inch",
    ),
) {

    sealed class IntroductionProcess(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Learn2earnEvents(
        category = "Introduction Process",
        event = event,
        params = params,
    ) {
        class ButtonLearn : IntroductionProcess("Button - Learn")
    }

    sealed class MainScreen(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Learn2earnEvents(
        category = "Main Screen",
        event = event,
        params = params,
    ) {
        class NoticeLear2earn(clientType: AnalyticsParam.ClientType) : MainScreen(
            event = "Notice - Learn&Earn",
            params = mapOf(AnalyticsParam.CLIENT_TYPE to clientType.value),
        )

        class NoticeClaimSuccess : MainScreen("Notice - Claim Successed")
    }

    sealed class PromoScreen(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Learn2earnEvents(
        category = "Promo Screen",
        event = event,
        params = params,
    ) {
        class ButtonBuy : PromoScreen("Button - Buy")

        class SuccessScreenOpened(clientType: AnalyticsParam.ClientType) : PromoScreen(
            event = "Success Screen Opened",
            params = mapOf(AnalyticsParam.CLIENT_TYPE to clientType.value),
        )
    }
}

internal sealed class AnalyticsParam {
    sealed class ClientType(val value: String) {
        class New : ClientType("New")
        class Old : ClientType("Old")
    }

    companion object Key {
        const val CLIENT_TYPE = "Client type"
    }
}
