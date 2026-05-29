package com.tangem.features.survey.impl.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.features.survey.SurveyFeatureToggles
import com.tangem.features.survey.deeplink.SurveyDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSurveyDeepLinkHandler @AssistedInject constructor(
    @Assisted private val queryParams: Map<String, String>,
    private val surveyFeatureToggles: SurveyFeatureToggles,
    private val appRouter: AppRouter,
) : SurveyDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        if (!surveyFeatureToggles.areSurveysEnabled) {
            TangemLogger.i("$TAG: survey deeplink ignored, feature is disabled")
            return
        }

        val token = queryParams[QUERY_TOKEN]?.takeIf { it.isNotBlank() }
        if (token == null) {
            TangemLogger.e("$TAG: survey deeplink ignored, missing 'token' query param")
            return
        }

        appRouter.push(AppRoute.Survey(token = token, displayId = queryParams[QUERY_DISPLAY_ID]))
    }

    @AssistedFactory
    interface Factory : SurveyDeepLinkHandler.Factory {
        override fun create(queryParams: Map<String, String>): DefaultSurveyDeepLinkHandler
    }

    private companion object {
        const val TAG = "SurveyDeepLink"
        const val QUERY_TOKEN = "token"
        const val QUERY_DISPLAY_ID = "display_id"
    }
}