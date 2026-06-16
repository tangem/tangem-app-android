package com.tangem.datasource.local.config.environment.models

import kotlinx.serialization.Serializable

@Serializable
data class ExpressModel(val apiKey: String, val signVerifierPublicKey: String)

@Serializable
data class P2PKeys(val mainnet: String, val hoodi: String)

data class SurveySparrowSwapRatingConfig(
    val surveyId: Long,
    val ratingQuestionId: Long,
    val feedbackQuestionId: Long,
)