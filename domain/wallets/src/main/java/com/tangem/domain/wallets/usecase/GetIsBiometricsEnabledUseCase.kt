package com.tangem.domain.wallets.usecase

import com.tangem.sdk.api.TangemSdkManager
import javax.inject.Inject

class GetIsBiometricsEnabledUseCase @Inject constructor(
    private val tangemSdkManager: TangemSdkManager,
) {

    operator fun invoke(): Boolean = runCatching(tangemSdkManager::needEnrollBiometrics).getOrNull() ?: false
}