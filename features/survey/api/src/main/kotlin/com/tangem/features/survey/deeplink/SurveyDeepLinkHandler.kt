package com.tangem.features.survey.deeplink

interface SurveyDeepLinkHandler {

    interface Factory {
        fun create(queryParams: Map<String, String>): SurveyDeepLinkHandler
    }
}