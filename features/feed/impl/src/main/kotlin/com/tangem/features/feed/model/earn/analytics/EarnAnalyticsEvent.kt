package com.tangem.features.feed.model.earn.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.IS_NOT_HTTP_ERROR
import com.tangem.core.analytics.models.OneTimePerSessionEvent

internal sealed class EarnAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Earn", event = event, params = params) {

    class EarnOpened : EarnAnalyticsEvent(event = "Page Opened")

    class MostlyUsedCarouselScrolled : EarnAnalyticsEvent(event = "Mostly Used Carousel Scrolled"),
        OneTimePerSessionEvent {
        override val oneTimeEventId: String = event
    }

    data class BestOpportunitiesFilterNetworkApplied(
        private val networkId: String?,
        private val filterType: FilterNetworkAnalytic,
    ) : EarnAnalyticsEvent(
        event = "Best Opportunities Filter Network Applied",
        params = mapOf(
            "Network Filter Type" to filterType.value,
            "Network Id" to networkId.orEmpty(),
        ),
    )

    data class BestOpportunitiesFilterTypeApplied(
        private val filterTypeAnalytic: FilterTypeAnalytic,
    ) : EarnAnalyticsEvent(
        event = "Best Opportunities Filter Type Applied",
        params = mapOf("Type" to filterTypeAnalytic.value),
    )

    data class OpportunitySelected(
        private val tokenSymbol: String,
        private val blockchain: String,
        private val source: String,
    ) : EarnAnalyticsEvent(
        event = "Opportunity selected",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to tokenSymbol,
            AnalyticsParam.BLOCKCHAIN to blockchain,
            AnalyticsParam.SOURCE to source,
        ),
    )

    data class AddTokenScreenOpened(
        private val tokenSymbol: String,
        private val blockchain: String,
        private val source: String,
    ) : EarnAnalyticsEvent(
        event = "Add Token Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to tokenSymbol,
            AnalyticsParam.BLOCKCHAIN to blockchain,
            AnalyticsParam.SOURCE to source,
        ),
    )

    data class TokenAdded(
        private val tokenSymbol: String,
        private val blockchain: String,
    ) : EarnAnalyticsEvent(
        event = "Token Added",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to tokenSymbol,
            AnalyticsParam.BLOCKCHAIN to blockchain,
        ),
    )

    data class BestOpportunitiesLoadError(
        private val code: Int?,
        private val message: String,
    ) : EarnAnalyticsEvent(
        event = "Best Opportunities Load Error",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )
}

internal const val BEST_OPPORTUNITIES_SOURCE = "Best Opportunity"
internal const val MOSTLY_USED_SOURCE = "Mostly Used"

internal enum class FilterNetworkAnalytic(val value: String) {
    ALL_NETWORKS("All Networks"),
    MY_NETWORKS("My Networks"),
    SPECIFIC("Specific"),
}

internal enum class FilterTypeAnalytic(val value: String) {
    YIELD("Yield"),
    STAKING("Staking"),
    ALL_TYPES("All Types"),
}