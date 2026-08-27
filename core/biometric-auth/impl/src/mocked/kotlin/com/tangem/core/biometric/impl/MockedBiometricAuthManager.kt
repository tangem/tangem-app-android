package com.tangem.core.biometric.impl

import com.tangem.core.biometric.BiometricAuthManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MockedBiometricAuthManager @Inject constructor() : BiometricAuthManager {

    override suspend fun authenticate(config: BiometricAuthManager.Config): BiometricAuthManager.Result =
        BiometricAuthManager.Result.Success
}