package com.tangem.feature.learn2earn.data.api

import com.tangem.datasource.api.promotion.models.*
import com.tangem.feature.learn2earn.data.models.PromoUserData
import com.tangem.feature.learn2earn.data.toggles.Learn2earnFeatureToggleManager

/**
[REDACTED_AUTHOR]
 */
interface Learn2earnRepository {

    val featureToggleManager: Learn2earnFeatureToggleManager

    fun getUserData(): PromoUserData

    fun updateUserData(userData: PromoUserData)

    fun getProgramName(): String

    suspend fun getPromotionInfo(): Result<PromotionInfoResponse>

    suspend fun validate(walletId: String): ValidateResponse

    suspend fun requestAward(walletId: String, address: String): AwardResponse

    suspend fun validateCode(walletId: String, code: String?): CodeValidateResponse

    suspend fun requestAwardByCode(walletId: String, address: String, code: String?): CodeAwardResponse
}