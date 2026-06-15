package com.tangem.features.survey.impl

import android.app.Activity
import com.surveysparrow.ss_android_sdk.SsSurvey
import com.surveysparrow.ss_android_sdk.SurveySparrow
import com.tangem.features.survey.SurveyLaunchData
import com.tangem.features.survey.SurveySparrowLauncher
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultSurveySparrowLauncher @Inject constructor() : SurveySparrowLauncher {

    override fun present(activity: Activity, data: SurveyLaunchData) {
        if (activity.isFinishing || activity.isDestroyed) {
            TangemLogger.e("$TAG: cannot present survey, activity is finishing/destroyed")
            return
        }

        val survey = try {
            SsSurvey(data.domain, data.token).apply {
                setSurveyType(SurveySparrow.CLASSIC)
                data.customParams.forEach { (key, value) -> addCustomParam(key, value) }
            }
        } catch (e: Exception) {
            TangemLogger.e("$TAG: failed to create SurveySparrow survey", e)
            return
        }

        // Result handling (onActivityResult -> [Survey] Completed/Dismissed) is planned in [REDACTED_TASK_KEY]
        SurveySparrow(activity, survey).startSurveyForResult(SURVEY_REQUEST_CODE)
        TangemLogger.d("$TAG: survey started (requestCode=$SURVEY_REQUEST_CODE)")
    }

    private companion object {
        const val TAG = "SurveySparrowPresenter"
        const val SURVEY_REQUEST_CODE = 1001
    }
}