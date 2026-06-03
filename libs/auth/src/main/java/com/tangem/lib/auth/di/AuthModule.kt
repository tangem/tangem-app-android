package com.tangem.lib.auth.di

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Moshi
import com.tangem.common.services.secure.SecureStorage
import com.tangem.datasource.api.auth.AuthApi
import com.tangem.datasource.api.auth.qualifier.SessionAuthAuthenticator
import com.tangem.datasource.api.auth.qualifier.SessionAuthInterceptor
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.devicekey.internal.DefaultDeviceKeyManager
import com.tangem.lib.auth.devicekey.internal.DisabledDeviceKeyManager
import com.tangem.lib.auth.dpop.DpopProofFactory
import com.tangem.lib.auth.dpop.internal.DefaultDpopProofFactory
import com.tangem.lib.auth.dpop.internal.DisabledDpopProofFactory
import com.tangem.lib.auth.http.DpopAuthorizationInterceptor
import com.tangem.lib.auth.http.SessionAuthenticator
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.nonce.internal.DefaultAuthNonceDecryptor
import com.tangem.lib.auth.nonce.internal.DisabledAuthNonceDecryptor
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.lib.auth.session.internal.AuthErrorConverter
import com.tangem.lib.auth.session.internal.DefaultSessionTokenRefresher
import com.tangem.lib.auth.session.internal.DefaultSessionTokensStore
import com.tangem.lib.auth.session.internal.DisabledSessionTokenRefresher
import com.tangem.lib.auth.session.internal.DisabledSessionTokensStore
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.logging.TangemLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import java.security.KeyStore
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AuthModule {

    /**
     * Exposes the backend-authentication feature toggle as a plain `Boolean` so that callers
     * in `core:datasource` (which can't depend on `libs:auth` for layering reasons) can gate
     * session-auth wiring without importing [AuthFeatureToggles].
     */
    @Provides
    @Named("isBackendAuthenticationEnabled")
    fun provideIsBackendAuthenticationEnabled(authFeatureToggles: AuthFeatureToggles): Boolean =
        authFeatureToggles.isBackendAuthenticationEnabled

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

    @Provides
    @Singleton
    fun provideSessionTokensStore(
        authFeatureToggles: AuthFeatureToggles,
        @ApplicationContext context: Context,
        @NetworkMoshi moshi: Moshi,
        dispatchers: CoroutineDispatcherProvider,
    ): SessionTokensStore {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return DisabledSessionTokensStore

        return runCatching {
            val storage: SecureStorage = AndroidSecureStorageV2(
                appContext = context,
                useStrongBox = false,
                name = "tangem_session_tokens",
            )
            DefaultSessionTokensStore(storage, moshi, dispatchers)
        }.getOrElse { e ->
            TangemLogger.e("Failed to init DefaultSessionTokensStore, falling back to disabled store", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            DisabledSessionTokensStore
        }
    }

    @Provides
    @Singleton
    fun provideDpopProofFactory(
        authFeatureToggles: AuthFeatureToggles,
        deviceKeyManager: DeviceKeyManager,
        dispatchers: CoroutineDispatcherProvider,
    ): DpopProofFactory {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return DisabledDpopProofFactory

        return DefaultDpopProofFactory(
            deviceKeyManager = deviceKeyManager,
            json = Json.Default,
            clock = Clock.System,
            dispatchers = dispatchers,
        )
    }

    @Suppress("LongParameterList")
    @Provides
    @Singleton
    fun provideSessionTokenRefresher(
        authFeatureToggles: AuthFeatureToggles,
        authApi: AuthApi,
        store: SessionTokensStore,
        deviceKeyManager: DeviceKeyManager,
        nonceDecryptor: AuthNonceDecryptor,
        appInfoProvider: AppInfoProvider,
        errorConverter: AuthErrorConverter,
        dispatchers: CoroutineDispatcherProvider,
    ): SessionTokenRefresher {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return DisabledSessionTokenRefresher

        return DefaultSessionTokenRefresher(
            authApi = authApi,
            store = store,
            deviceKeyManager = deviceKeyManager,
            nonceDecryptor = nonceDecryptor,
            appInfoProvider = appInfoProvider,
            errorConverter = errorConverter,
            clock = Clock.System,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    @SessionAuthInterceptor
    fun provideDpopAuthorizationInterceptor(store: SessionTokensStore, proofFactory: DpopProofFactory): Interceptor {
        return DpopAuthorizationInterceptor(store, proofFactory)
    }

    @Provides
    @Singleton
    @SessionAuthAuthenticator
    fun provideSessionAuthenticator(refresher: SessionTokenRefresher, proofFactory: DpopProofFactory): Authenticator {
        return SessionAuthenticator(refresher, proofFactory)
    }
}