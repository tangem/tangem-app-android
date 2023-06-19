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
    suspend fun requestAwardByCode(walletId: String, address: String): Result<Boolean>
    suspend fun getPromotionInfo(): Result<PromotionInfoResponse>
    suspend fun validate(walletId: String): ValidateResponse
    suspend fun award(walletId: String): AwardResponse
    suspend fun codeValidate(walletId: String, code: String?): CodeValidateResponse
    suspend fun codeAward(walletId: String, address: String, code: String?): CodeAwardResponse
}