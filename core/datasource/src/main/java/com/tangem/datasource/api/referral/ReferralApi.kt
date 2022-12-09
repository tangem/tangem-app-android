package com.tangem.datasource.api.referral

import com.tangem.datasource.api.referral.models.ReferralResponse
import com.tangem.datasource.api.referral.models.StartReferralBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Api for referral feature
 */
interface ReferralApi {

    /** Returns referral status by [walletId] */
    @GET("referral/{walletId}")
    suspend fun getReferralStatus(@Path("walletId") walletId: String): ReferralResponse

    /** Make user referral, requires [StartReferralBody] */
    @POST("referral")
    suspend fun startReferral(@Body startReferralBody: StartReferralBody): ReferralResponse
}
