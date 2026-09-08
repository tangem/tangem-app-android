package com.tangem.store.datasource.config

/**
 * Store-owned slice of the application environment config: analytics/attribution/marketing keys, the
 * SurveySparrow swap-rating survey, and the Tangem Tech API keys used by the auth provider — so
 * consumers do not depend on the full environment config.
 */
data class StoreEnvironmentConfig(
    val amplitudeApiKey: String,
    val amplitudeApiKeyDev: String?,
    val appsFlyerApiKey: String,
    val appsAppId: String,
    val customerIoCdpApiKey: String?,
    val surveySparrowToken: String?,
    val surveySparrowSwapRating: SurveySparrowSwapRating?,
    val blockAidApiKey: String?,
    val tangemApiKey: String?,
    val tangemApiKeyDev: String?,
    val tangemApiKeyStage: String?,
) {

    data class SurveySparrowSwapRating(
        val surveyId: Long,
        val ratingQuestionId: Long,
        val feedbackQuestionId: Long,
    )
}