package com.tangem.feature.tester.presentation.surveysparrow

import android.app.Activity
import com.surveysparrow.ss_android_sdk.SsSurvey
import com.surveysparrow.ss_android_sdk.SurveySparrow
import com.tangem.utils.logging.TangemLogger

/**
 * Manager for Survey Sparrow SDK.
 *
 * @param domain Survey Sparrow domain (e.g., "yourcompany")
 * @param token Survey Sparrow SDK token
 */
class SurveySparrowManager(
    private val domain: String,
    private val token: String,
) {

    /**
     * Create a SurveySparrow instance to start a survey.
     *
     * @param activity The activity context
     * @param customVariables Optional custom variables to pass to the survey
     * @return SurveySparrow instance ready to start
     */
    fun createSurvey(activity: Activity, customVariables: Map<String, String>? = null): SurveySparrow? {
        return try {
            val survey = SsSurvey(domain, token).apply {
                customVariables?.forEach { (key, value) ->
                    addCustomParam(key, value)
                }
            }

            SurveySparrow(activity, survey)
        } catch (e: Exception) {
            TangemLogger.e("Failed to create SurveySparrow survey", e)
            null
        }
    }

    /**
     * Start a survey for result.
     *
     * @param activity The activity context
     * @param requestCode The request code for onActivityResult
     * @param customVariables Optional custom variables to pass to the survey
     */
    fun startSurveyForResult(activity: Activity, requestCode: Int, customVariables: Map<String, String>? = null) {
        val surveySparrow = createSurvey(activity, customVariables)
        if (surveySparrow != null) {
            surveySparrow.startSurveyForResult(requestCode)
            TangemLogger.d("SurveySparrow survey started with requestCode: $requestCode")
        }
    }
}