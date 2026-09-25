package com.tangem.domain.appsflyer.repository

interface AppsFlyerRepository {

    suspend fun getDeeplink(): String?

    suspend fun clearDeeplink()

    /** Forgets the referral attribution (refcode / campaign) stored from a conversion or a OneLink click. */
    suspend fun clearReferralAttribution()
}