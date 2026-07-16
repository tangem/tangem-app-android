package com.tangem.domain.appsflyer.usecase

import com.tangem.domain.appsflyer.AppsFlyerDeeplinkSource
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository

/**
 * Returns whether the app install was attributed to an AppsFlyer referral deep link.
 */
class IsReferralInstallUseCase(
    private val appsFlyerRepository: AppsFlyerRepository,
) {
    suspend operator fun invoke(): Boolean {
        return appsFlyerRepository.getDeeplink(AppsFlyerDeeplinkSource.Referral) != null
    }
}