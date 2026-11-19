package com.tangem.features.survey.impl.nativesurvey.repository

import com.tangem.utils.SupportedLanguages
import javax.inject.Inject

internal class SurveyLanguageProvider @Inject constructor() {

    fun current(): String = SupportedLanguages.getCurrentSupportedLanguageCode()

    companion object {
        const val BASE_LANGUAGE = SupportedLanguages.ENGLISH
    }
}