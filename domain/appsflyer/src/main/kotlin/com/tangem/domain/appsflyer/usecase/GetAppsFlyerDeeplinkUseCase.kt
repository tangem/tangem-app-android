package com.tangem.domain.appsflyer.usecase

import com.tangem.domain.appsflyer.AppsFlyerDeeplink
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository

/**
 * Returns the pending [AppsFlyerDeeplink] the app was opened by, or `null` when there is none.
 */
class GetAppsFlyerDeeplinkUseCase(
    private val appsFlyerRepository: AppsFlyerRepository,
) {
    suspend operator fun invoke(): AppsFlyerDeeplink? = AppsFlyerDeeplink.from(appsFlyerRepository.getDeeplink())
}