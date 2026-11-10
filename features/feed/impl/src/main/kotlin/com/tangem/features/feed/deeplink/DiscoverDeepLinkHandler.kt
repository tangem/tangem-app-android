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
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedSection
import javax.inject.Inject

internal class DiscoverDeepLinkHandler @Inject constructor(
    private val feedEntryController: FeedEntryController,
    private val appRouter: AppRouter,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val queryParams = request.queryParams
        // published before navigating: the hub reads the request when it is built, which on a cold
        // start is a consequence of the navigation below
        feedEntryController.request(
            FeedEntryRequest(
                section = FeedSection.parse(queryParams[SECTION_KEY]),
                categoryId = queryParams[CATEGORY_KEY]?.takeIf(String::isNotBlank),
                earnType = PreselectedEarnType.parse(queryParams[TYPE_KEY]),
            ),
        )

        appRouter.popToIfCovered(AppRoute.Wallet)
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://discover"
    }
}