package com.tangem.features.survey.impl.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class SurveyDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val queryParams = request.queryParams
        val token = queryParams[QUERY_TOKEN]?.takeIf { it.isNotBlank() }
        if (token == null) {
            TangemLogger.e("$TAG: survey deeplink ignored, missing 'token' query param")
            return
        }

        appRouter.push(AppRoute.Survey(token = token, displayId = queryParams[QUERY_DISPLAY_ID]))
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://survey"

        private const val TAG = "SurveyDeepLink"
        private const val QUERY_TOKEN = "token"
        private const val QUERY_DISPLAY_ID = "display_id"
    }
}