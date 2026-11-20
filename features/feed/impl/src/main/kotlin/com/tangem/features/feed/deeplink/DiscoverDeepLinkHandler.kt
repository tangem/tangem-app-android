package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.SECTION_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.utils.popToIfCovered
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedFilterSheet
import com.tangem.features.feed.v2.FeedSection
import javax.inject.Inject

internal class DiscoverDeepLinkHandler @Inject constructor(
    private val feedEntryController: FeedEntryController,
    private val appRouter: AppRouter,
    private val feedFeatureToggles: FeedFeatureToggles,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        // published before navigating: the hub reads the request when it is built, which on a cold
        // start is a consequence of the navigation below
        feedEntryController.request(createEntryRequest(queryParams = request.queryParams))

        appRouter.popToIfCovered(AppRoute.Wallet)
    }

    private fun createEntryRequest(queryParams: Map<String, String>): FeedEntryRequest {
        val rawSection = queryParams[SECTION_KEY]
        val section = FeedSection.parse(rawSection)
        val request = FeedEntryRequest(
            section = section,
            categoryId = queryParams[CATEGORY_KEY]?.takeIf(String::isNotBlank),
            earnType = PreselectedEarnType.parse(queryParams[TYPE_KEY]),
        )

        if (!feedFeatureToggles.isDiscoverDeeplinkFiltersEnabled) return request

        if (rawSection == FOR_YOU_SECTION) return FeedEntryRequest(isForYou = true)

        return request.copy(sheet = FeedFilterSheet.resolve(section = section, value = queryParams[FILTER_KEY]))
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://discover"

        private const val FOR_YOU_SECTION = "foryou"
        const val FILTER_KEY = "filter"
    }
}