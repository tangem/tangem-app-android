package com.tangem.spend.datasource.di

import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.Timeouts
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.spend.datasource.config.TangemPay
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.TangemPayAuthApi
import com.tangem.spend.datasource.visa.VisaApi
import com.tangem.utils.info.AppInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SpendDatasourceModule {

    private const val TIMEOUT_60_SECONDS = 60L

    @Provides
    @IntoMap
    @StringKey(TangemPay.Bff.KEY)
    fun provideTangemPayBffConfig(environmentConfig: EnvironmentConfig, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemPay.Bff(environmentConfig, appInfoProvider)
    }

    @Provides
    @IntoMap
    @StringKey(TangemPay.Auth.KEY)
    fun provideTangemPayAuthConfig(environmentConfig: EnvironmentConfig, appInfoProvider: AppInfoProvider): ApiConfig {
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
            ),
        )
    }

    @Provides
    @Singleton
    fun provideVisaApi(factory: RetrofitFactory): VisaApi {
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
            ),
        )
    }
}