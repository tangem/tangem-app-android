package com.tangem.domain.appsflyer.usecase

import com.tangem.domain.appsflyer.repository.AppsFlyerRepository

class ClearAppsFlyerReferralAttributionUseCase(
    private val appsFlyerRepository: AppsFlyerRepository,
) {
    suspend operator fun invoke() {
        appsFlyerRepository.clearReferralAttribution()
    }
}
