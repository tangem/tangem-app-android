package com.tangem.features.introduction.impl.model

import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class IntroductionReferral @Inject constructor(
    private val appsFlyerRepository: AppsFlyerRepository,
) {

    suspend fun readId(): String? = runSuspendCatching { appsFlyerRepository.getConversionData()?.refcode }
        .onFailure { error -> TangemLogger.e("Failed to read the referral for the Introduction analytics", error) }
        .getOrNull()
}