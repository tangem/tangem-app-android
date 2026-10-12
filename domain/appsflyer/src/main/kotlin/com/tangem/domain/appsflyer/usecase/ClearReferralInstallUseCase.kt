package com.tangem.domain.appsflyer.usecase

import com.tangem.domain.appsflyer.AppsFlyerDeeplink
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository

class ClearReferralInstallUseCase(
    private val appsFlyerRepository: AppsFlyerRepository,
) {
    suspend operator fun invoke() {
        val stored = appsFlyerRepository.getNavigationDeeplink()

        if (AppsFlyerDeeplink.from(stored) == AppsFlyerDeeplink.Referral) {
            appsFlyerRepository.clearNavigationDeeplink()
        }
    }
}