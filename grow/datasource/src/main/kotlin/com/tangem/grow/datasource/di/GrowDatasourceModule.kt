package com.tangem.grow.datasource.di

import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.Timeouts
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.grow.datasource.config.Express
import com.tangem.grow.datasource.config.GaslessTxService
import com.tangem.grow.datasource.config.GrowEnvironmentConfig
import com.tangem.grow.datasource.config.MoonPay
import com.tangem.grow.datasource.express.ExpressAuthProvider
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.grow.datasource.gasless.GaslessTxServiceApi
import com.tangem.grow.datasource.gasless.GaslessTxServiceApiV2
import com.tangem.grow.datasource.gasless.TronGaslessApi
import com.tangem.grow.datasource.moonpay.MoonPayApi
import com.tangem.grow.datasource.onramp.OnrampApi
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
internal object GrowDatasourceModule {

    private const val TIMEOUT_60_SECONDS = 60L

    @Provides
    @IntoMap
    @StringKey(MoonPay.KEY)
    fun provideMoonPayConfig(): ApiConfig {
        return MoonPay()
    }

    @Provides
    @Singleton
    fun provideMoonPayApi(factory: RetrofitFactory): MoonPayApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = MoonPay.ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @IntoMap
    @StringKey(GaslessTxService.KEY)
    fun provideGaslessConfig(
        growEnvironmentConfig: GrowEnvironmentConfig,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return GaslessTxService(
            growEnvironmentConfig = growEnvironmentConfig,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @Singleton
    fun provideGaslessTxServiceApi(factory: RetrofitFactory): GaslessTxServiceApi {
        return factory.build(gaslessApiSpec())
    }

    @Provides
    @Singleton
    fun provideGaslessTxServiceApiV2(factory: RetrofitFactory): GaslessTxServiceApiV2 {
        return factory.build(gaslessApiSpec())
    }

    @Provides
    @Singleton
    fun provideTronGaslessApi(factory: RetrofitFactory): TronGaslessApi {
        return factory.build(gaslessApiSpec())
    }

    private fun gaslessApiSpec() = RetrofitApiSpec(
        apiConfigId = GaslessTxService.ID,
        shouldApplyTimeoutAnnotations = false,
        shouldUseSessionAuth = false,
        timeouts = Timeouts(
            callTimeoutSeconds = TIMEOUT_60_SECONDS,
            connectTimeoutSeconds = TIMEOUT_60_SECONDS,
            readTimeoutSeconds = TIMEOUT_60_SECONDS,
            writeTimeoutSeconds = TIMEOUT_60_SECONDS,
        ),
    )

    @Provides
    @IntoMap
    @StringKey(Express.KEY)
    fun provideExpressConfig(
        growEnvironmentConfig: GrowEnvironmentConfig,
        expressAuthProvider: ExpressAuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return Express(
            growEnvironmentConfig = growEnvironmentConfig,
            expressAuthProvider = expressAuthProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @Singleton
    fun provideExpressApi(factory: RetrofitFactory): TangemExpressApi {
        return factory.build(expressApiSpec())
    }

    @Provides
    @Singleton
    fun provideOnrampApi(factory: RetrofitFactory): OnrampApi {
        return factory.build(expressApiSpec())
    }

    private fun expressApiSpec() = RetrofitApiSpec(
        apiConfigId = Express.ID,
        shouldApplyTimeoutAnnotations = false,
        shouldUseSessionAuth = false,
    )
}