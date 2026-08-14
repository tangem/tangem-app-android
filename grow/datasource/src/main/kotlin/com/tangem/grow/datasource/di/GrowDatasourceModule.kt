package com.tangem.grow.datasource.di

import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.build
import com.tangem.core.remote.config.ApiConfig
import com.tangem.grow.datasource.config.MoonPay
import com.tangem.grow.datasource.moonpay.MoonPayApi
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
}