package com.tangem.features.survey

import android.app.Activity

interface SurveySparrowLauncher {

    fun present(activity: Activity, data: SurveyLaunchData)
}

data class SurveyLaunchData(
    val domain: String,
    val token: String,
    val customParams: Map<String, String>,
)