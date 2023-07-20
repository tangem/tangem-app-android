package com.tangem.feature.learn2earn.data.models

/**
 * @author Anton Zhilenkov on 21.06.2023.
 */
data class PromoUserData(
    val promoCode: String?,
    val isLearningStageFinished: Boolean,
    val isRegisteredInPromotion: Boolean,
    val isAlreadyReceivedAward: Boolean,
)
