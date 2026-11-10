package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.EARN_TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.domain.models.earn.PreselectedEarnType
import javax.inject.Inject

internal class EarnDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val queryParams = request.queryParams
        val earnType = PreselectedEarnType.parse(queryParams[EARN_TYPE_KEY])
        val networkId = queryParams[NETWORK_ID_KEY]?.takeIf { it.isNotBlank() }

        appRouter.push(
            AppRoute.Earn(
                preselectedEarnType = earnType,
                preselectedNetworkId = networkId,
            ),
        )
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://earn"
    }
}