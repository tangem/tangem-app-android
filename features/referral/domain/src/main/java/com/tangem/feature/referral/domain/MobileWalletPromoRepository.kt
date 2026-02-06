package com.tangem.feature.referral.domain

interface MobileWalletPromoRepository {

    suspend fun shouldShowMobileWalletPromo(): Boolean

    suspend fun setShouldShowMobileWalletPromo(value: Boolean)
}