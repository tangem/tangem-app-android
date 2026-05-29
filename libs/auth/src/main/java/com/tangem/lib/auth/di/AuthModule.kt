package com.tangem.lib.auth.di

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.devicekey.internal.DefaultDeviceKeyManager
import com.tangem.lib.auth.devicekey.internal.DisabledDeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.nonce.internal.DefaultAuthNonceDecryptor
import com.tangem.lib.auth.nonce.internal.DisabledAuthNonceDecryptor
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AuthModule {

    @Provides
    @Singleton
    fun provideDeviceKeyManager(
        authFeatureToggles: AuthFeatureToggles,
        dispatchers: CoroutineDispatcherProvider,
    ): DeviceKeyManager {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return DisabledDeviceKeyManager

        return runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            DefaultDeviceKeyManager(keyStore, dispatchers)
        }.getOrElse { e ->
            TangemLogger.e("Failed to init AndroidKeyStore, falling back to disabled DeviceKeyManager", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            DisabledDeviceKeyManager
        }
    }

    @Provides
    @Singleton
    fun provideAuthNonceDecryptor(
        authFeatureToggles: AuthFeatureToggles,
        @Named("authServiceKey") authServiceKey: String?,
        dispatchers: CoroutineDispatcherProvider,
    ): AuthNonceDecryptor {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return DisabledAuthNonceDecryptor

        if (authServiceKey.isNullOrEmpty()) return DisabledAuthNonceDecryptor

        return runCatching { DefaultAuthNonceDecryptor(authServiceKey, dispatchers) }
            .getOrElse { e ->
                TangemLogger.e("Failed to create AuthNonceDecryptor", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                DisabledAuthNonceDecryptor
            }
    }
}