package com.tangem.domain.appsflyer.usecase

import com.tangem.domain.appsflyer.AppsFlyerDeeplinkSource
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository

class ClearAppsFlyerDeeplinkUseCase(
    private val appsFlyerRepository: AppsFlyerRepository,
) {
    suspend operator fun invoke(source: AppsFlyerDeeplinkSource) {
        appsFlyerRepository.clearDeeplink(source)
    }
}