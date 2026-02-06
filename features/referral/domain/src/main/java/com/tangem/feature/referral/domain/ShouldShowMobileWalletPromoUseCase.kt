package com.tangem.feature.referral.domain

import javax.inject.Inject

class ShouldShowMobileWalletPromoUseCase @Inject constructor(
    private val mobileWalletPromoRepository: MobileWalletPromoRepository,
) {

    suspend operator fun invoke(): Boolean {
        return mobileWalletPromoRepository.shouldShowMobileWalletPromo()
    }
}