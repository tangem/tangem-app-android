package com.tangem.feature.learn2earn.data.api

import com.tangem.datasource.api.promotion.models.*

/**
[REDACTED_AUTHOR]
 */
interface Learn2earnRepository {

    fun isHadActivatedCards(): Boolean

    fun getPromoCode(): String?

    fun savePromoCode(code: String)

    fun getProgramName(): String

    fun isAlreadyReceivedAward(): Boolean

    suspend fun getPromotionInfo(): Result<PromotionInfoResponse>

    suspend fun validate(walletId: String): ValidateResponse

    suspend fun requestAward(walletId: String): AwardResponse

    suspend fun validateCode(walletId: String, code: String?): CodeValidateResponse

    suspend fun requestAwardByCode(walletId: String, address: String, code: String?): CodeAwardResponse
}