package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.EARN_TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.entry.deeplink.EarnDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultEarnDeepLinkHandler @AssistedInject constructor(
    @Assisted private val queryParams: Map<String, String>,
    private val appRouter: AppRouter,
) : EarnDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val earnType = PreselectedEarnType.parse(queryParams[EARN_TYPE_KEY])
        val networkId = queryParams[NETWORK_ID_KEY]?.takeIf { it.isNotBlank() }

        appRouter.push(
            AppRoute.Earn(
                preselectedEarnType = earnType,
                preselectedNetworkId = networkId,
            ),
        )
    }

    @AssistedFactory
    interface Factory : EarnDeepLinkHandler.Factory {
        override fun create(queryParams: Map<String, String>): DefaultEarnDeepLinkHandler
    }
}