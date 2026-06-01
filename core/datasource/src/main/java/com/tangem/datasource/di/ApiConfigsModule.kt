package com.tangem.datasource.di

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.*
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.api.auth.ExpressAuthProvider
import com.tangem.datasource.api.auth.P2PEthPoolAuthProvider
import com.tangem.datasource.api.auth.StakeKitAuthProvider
import com.tangem.utils.info.AppInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object ApiConfigsModule {

    @Provides
    @IntoSet
    fun provideExpressConfig(
        environmentConfig: EnvironmentConfig,
        expressAuthProvider: ExpressAuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return Express(
            environmentConfig = environmentConfig,
            expressAuthProvider = expressAuthProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoSet
    fun provideStakeKitConfig(stakeKitAuthProvider: StakeKitAuthProvider): ApiConfig {
        return StakeKit(stakeKitAuthProvider)
    }

    @Provides
    @IntoSet
    fun provideP2PEthPoolConfig(p2pAuthProvider: P2PEthPoolAuthProvider): ApiConfig {
        return P2PEthPool(p2pAuthProvider)
    }

    @Provides
    @IntoSet
    fun provideTangemTechConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemTech(
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoSet
    fun provideNewsConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return News(
            appInfoProvider = appInfoProvider,
            authProvider = authProvider,
        )
    }

    @Provides
    @IntoSet
    fun provideYieldSupplyConfig(
        environmentConfig: EnvironmentConfig,
        authProvider: AuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return YieldSupply(
            environmentConfig = environmentConfig,
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoSet
    fun provideTangemPayBffConfig(environmentConfig: EnvironmentConfig, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemPay.Bff(environmentConfig, appInfoProvider)
    }

    @Provides
    @IntoSet
    fun provideTangemPayAuthConfig(environmentConfig: EnvironmentConfig, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemPay.Auth(environmentConfig, appInfoProvider)
    }

    @Provides
    @IntoSet
    fun provideBlockAidConfig(environmentConfig: EnvironmentConfig): ApiConfig {
        return BlockAid(environmentConfig)
    }

    @Provides
    @IntoSet
    fun provideMoonPayConfig(): ApiConfig {
        return MoonPay()
    }

    @Provides
    @IntoSet
    fun provideGaslessServiceConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return GaslessTxService(
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoSet
    fun provideSurveySparrowConfig(environmentConfig: EnvironmentConfig): ApiConfig {
        return SurveySparrow(environmentConfig)
    }
}