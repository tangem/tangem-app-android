package com.tangem.domain.appsflyer.usecase

import com.tangem.domain.appsflyer.repository.AppsFlyerRepository

class ClearAppsFlyerDeeplinkUseCase(
    private val appsFlyerRepository: AppsFlyerRepository,
) {
    suspend operator fun invoke() {
        appsFlyerRepository.clearDeeplink()
    }
}