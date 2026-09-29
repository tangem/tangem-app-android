package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.SECTION_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.utils.popToIfCovered
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.entry.deeplink.DiscoverDeepLinkHandler
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedSection
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultDiscoverDeepLinkHandler @AssistedInject constructor(
    @Assisted private val queryParams: Map<String, String>,
    private val feedEntryController: FeedEntryController,
    private val feedFeatureToggles: FeedFeatureToggles,
    private val appRouter: AppRouter,
) : DiscoverDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        if (feedFeatureToggles.isNewShtorkaEnabled) {
            // published before navigating: the hub reads the request when it is built, which on a cold
            // start is a consequence of the navigation below
            feedEntryController.request(
                FeedEntryRequest(
                    section = FeedSection.parse(queryParams[SECTION_KEY]),
                    categoryId = queryParams[CATEGORY_KEY]?.takeIf(String::isNotBlank),
                    earnType = PreselectedEarnType.parse(queryParams[TYPE_KEY]),
                ),
            )
        } else {
            TangemLogger.i("Discover deeplink: the feed hub is disabled, opening the wallet instead")
        }

        appRouter.popToIfCovered(AppRoute.Wallet)
    }

    @AssistedFactory
    interface Factory : DiscoverDeepLinkHandler.Factory {
        override fun create(queryParams: Map<String, String>): DefaultDiscoverDeepLinkHandler
    }
}