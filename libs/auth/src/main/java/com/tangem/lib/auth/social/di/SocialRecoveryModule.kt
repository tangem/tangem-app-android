package com.tangem.lib.auth.social.di

import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.build
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
import com.tangem.lib.auth.social.internal.SocialRecoveryApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SocialRecoveryModule {

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
        dispatchers: CoroutineDispatcherProvider,
    ): SocialRecoveryClient {
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