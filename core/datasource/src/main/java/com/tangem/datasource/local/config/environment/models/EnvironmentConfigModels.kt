package com.tangem.datasource.local.config.environment.models

data class ExpressModel(val apiKey: String, val signVerifierPublicKey: String)

data class P2PKeys(val mainnet: String, val hoodi: String)

data class SurveySparrowSwapRatingConfig(
    val surveyId: Long,
    val ratingQuestionId: Long,
    val feedbackQuestionId: Long,
)