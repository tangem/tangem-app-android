package com.tangem.features.survey.impl.nativesurvey.model

import com.tangem.common.routing.AppRoute

internal fun AppRoute.NativeSurvey.Source.toSurveySource(): SurveySource = when (this) {
    AppRoute.NativeSurvey.Source.SHEET -> SurveySource.SHEET
    AppRoute.NativeSurvey.Source.PUSH -> SurveySource.PUSH
    AppRoute.NativeSurvey.Source.BANNER -> SurveySource.BANNER
    AppRoute.NativeSurvey.Source.LINK -> SurveySource.LINK
}