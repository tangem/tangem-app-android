package com.tangem.spend.datasource.di

import com.tangem.core.remote.ApiSerialization
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.Timeouts
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.spend.datasource.config.TangemPay
import com.tangem.spend.datasource.config.TangemPayEnvironmentConfig
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.spend.datasource.tangempay.TangemPayOrderApi
import com.tangem.spend.datasource.tangempay.TangemPayAuthApi
import com.tangem.spend.datasource.tangempay.TangemPayProvisioningApi
import com.tangem.spend.datasource.tangempay.serialization.TangemPaySerializersModuleProvider
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.serialization.SerializersModuleProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SpendDatasourceModule {

    private const val TIMEOUT_60_SECONDS = 60L

    @Provides
    @IntoMap
    @StringKey(TangemPay.Bff.KEY)
    fun provideTangemPayBffConfig(
        environmentConfig: TangemPayEnvironmentConfig,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return TangemPay.Bff(environmentConfig, appInfoProvider)
    }

    @Provides
    @IntoMap
    @StringKey(TangemPay.Auth.KEY)
    fun provideTangemPayAuthConfig(
        environmentConfig: TangemPayEnvironmentConfig,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return TangemPay.Auth(environmentConfig, appInfoProvider)
    }

    @Provides
    @Singleton
    fun provideTangemPayApi(factory: RetrofitFactory): TangemPayApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemPay.Bff.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                timeouts = Timeouts(
                    callTimeoutSeconds = TIMEOUT_60_SECONDS,
                    connectTimeoutSeconds = TIMEOUT_60_SECONDS,
                    readTimeoutSeconds = TIMEOUT_60_SECONDS,
                ),
                serialization = ApiSerialization.KOTLINX,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayOrderApi(factory: RetrofitFactory): TangemPayOrderApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemPay.Bff.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                timeouts = Timeouts(
                    callTimeoutSeconds = TIMEOUT_60_SECONDS,
                    connectTimeoutSeconds = TIMEOUT_60_SECONDS,
                    readTimeoutSeconds = TIMEOUT_60_SECONDS,
                ),
                serialization = ApiSerialization.KOTLINX,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayProvisioningApi(factory: RetrofitFactory): TangemPayProvisioningApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemPay.Bff.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                timeouts = Timeouts(
                    callTimeoutSeconds = TIMEOUT_60_SECONDS,
                    connectTimeoutSeconds = TIMEOUT_60_SECONDS,
                    readTimeoutSeconds = TIMEOUT_60_SECONDS,
                ),
                shouldSaveLogs = false,
                isLoggable = false,
                serialization = ApiSerialization.KOTLINX,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayAuthApi(factory: RetrofitFactory): TangemPayAuthApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemPay.Auth.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
                serialization = ApiSerialization.KOTLINX,
            ),
        )
    }

    @Provides
    @IntoSet
    fun provideTangemPaySerializersModuleProvider(): SerializersModuleProvider {
        return TangemPaySerializersModuleProvider
    }
}