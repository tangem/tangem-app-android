package com.tangem.features.survey.impl.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.features.survey.impl.nativesurvey.opener.NativeSurveyOpener
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

/** `tangem://native_survey`: an invalid optional parameter counts as not passed (§6.1 of the spec). */
internal class NativeSurveyDeepLinkHandler @Inject constructor(
    private val opener: NativeSurveyOpener,
) : DeepLinkHandler {

    override val isEnabled: Boolean
        get() = opener.isAvailable

    override fun handle(request: DeepLinkRequest) {
        val surveyRequest = request.queryParams.toSurveyRequest()
        if (surveyRequest == null) {
            TangemLogger.i("Native survey deep link ignored: no valid '$QUERY_SURVEY_ID'")
            return
        }

        request.scope.launch { opener.open(surveyRequest, request.router) }
    }

    private fun Map<String, String>.toSurveyRequest(): NativeSurveyOpener.Request? {
        val surveyId = get(QUERY_SURVEY_ID)?.toLongOrNull()?.takeIf { it > 0 } ?: return null

        return NativeSurveyOpener.Request(
            surveyId = surveyId,
            source = when (get(QUERY_CHANNEL)) {
                CHANNEL_PUSH -> AppRoute.NativeSurvey.Source.PUSH
                CHANNEL_BANNER -> AppRoute.NativeSurvey.Source.BANNER
                else -> AppRoute.NativeSurvey.Source.LINK
            },
            stage = get(QUERY_STAGE).orEmpty(),
            repeatAfterDays = get(QUERY_REPEAT_AFTER_DAYS)?.toIntOrNull()?.takeIf { it >= 0 },
        )
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://native_survey"

        private const val QUERY_SURVEY_ID = "survey_id"
        private const val QUERY_STAGE = "stage"
        private const val QUERY_REPEAT_AFTER_DAYS = "repeat_after_days"
        private const val QUERY_CHANNEL = "channel"
        private const val CHANNEL_PUSH = "push"
        private const val CHANNEL_BANNER = "banner"
    }
}