package com.tangem.lib.auth.social.di

import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.lib.auth.api.AuthApi
import com.tangem.lib.auth.api.config.Auth
import com.tangem.lib.auth.attestation.AttestationProvider
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.internal.AuthErrorConverter
import com.tangem.lib.auth.session.internal.SignedRequestPayload
import com.tangem.lib.auth.social.SocialRecoveryClient
import com.tangem.lib.auth.social.internal.DefaultSocialRecoveryClient
import com.tangem.lib.auth.social.internal.MockSocialRecoveryClient
import com.tangem.lib.auth.social.internal.SocialRecoveryApi
import com.tangem.libs.auth.BuildConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SocialRecoveryModule {

    /**
     * Routes the whole social recovery transport to the on-device stand-in. Never in a build that can
     * reach real users: the stand-in keeps the binding in local preferences, so a wallet it reports as
     * backed up is recoverable on this device only.
     *
     * Only the mocked build, whose whole network layer is stubbed, still needs it — the service is
     * deployed, and leaving the debug and internal builds on the stand-in would have QA exercise the
     * fake instead of the contract.
     */
    private val shouldUseMockBackend: Boolean
        get() = BuildConfig.BUILD_TYPE == ApiConfig.MOCKED_BUILD_TYPE

    @Provides
    @Singleton
    fun provideSocialRecoveryApi(factory: RetrofitFactory): SocialRecoveryApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = Auth.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = true,
                // the bodies carry the provider id token; the saved logs are exportable by the user
                shouldSaveLogs = false,
            ),
        )
    }

    @Suppress("LongParameterList")
    @Provides
    @Singleton
    fun provideSocialRecoveryClient(
        api: SocialRecoveryApi,
        authApi: AuthApi,
        deviceKeyManager: DeviceKeyManager,
        nonceDecryptor: AuthNonceDecryptor,
        signedRequestPayload: SignedRequestPayload,
        attestationProvider: AttestationProvider,
        errorConverter: AuthErrorConverter,
        sessionTokensStore: SessionTokensStore,
        walletRegistrar: WalletRegistrar,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): SocialRecoveryClient {
        if (shouldUseMockBackend) {
            return MockSocialRecoveryClient(
                appPreferencesStore = appPreferencesStore,
                dispatchers = dispatchers,
            )
        }

        return DefaultSocialRecoveryClient(
            api = api,
            authApi = authApi,
            deviceKeyManager = deviceKeyManager,
            nonceDecryptor = nonceDecryptor,
            signedRequestPayload = signedRequestPayload,
            attestationProvider = attestationProvider,
            errorConverter = errorConverter,
            sessionTokensStore = sessionTokensStore,
            walletRegistrar = walletRegistrar,
            dispatchers = dispatchers,
        )
    }
}