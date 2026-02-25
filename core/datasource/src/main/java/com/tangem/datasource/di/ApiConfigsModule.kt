package com.tangem.datasource.di

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.*
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.P2PEthPoolAuthProvider
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider
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
        appVersionProvider: AppVersionProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return Express(
            environmentConfig = environmentConfig,
            expressAuthProvider = expressAuthProvider,
            appVersionProvider = appVersionProvider,
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
    fun provideTangemTechConfig(
        appVersionProvider: AppVersionProvider,
        authProvider: AuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig = TangemTech(
        appVersionProvider = appVersionProvider,
        authProvider = authProvider,
        appInfoProvider = appInfoProvider,
    )

    @Provides
    @IntoSet
    fun provideNewsConfig(
        appVersionProvider: AppVersionProvider,
        authProvider: AuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig = News(
        appVersionProvider = appVersionProvider,
        appInfoProvider = appInfoProvider,
        authProvider = authProvider,
    )

    @Provides
    @IntoSet
    fun provideYieldSupplyConfig(
        environmentConfig: EnvironmentConfig,
        appVersionProvider: AppVersionProvider,
        authProvider: AuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig = YieldSupply(
        environmentConfig = environmentConfig,
        appVersionProvider = appVersionProvider,
        authProvider = authProvider,
        appInfoProvider = appInfoProvider,
    )

    @Provides
    @IntoSet
    fun provideTangemPayBffConfig(
        environmentConfig: EnvironmentConfig,
        appVersionProvider: AppVersionProvider,
    ): ApiConfig = TangemPay.Bff(environmentConfig, appVersionProvider)

    @Provides
    @IntoSet
    fun provideTangemPayAuthConfig(
        environmentConfig: EnvironmentConfig,
        appVersionProvider: AppVersionProvider,
    ): ApiConfig = TangemPay.Auth(environmentConfig, appVersionProvider)

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
    fun provideGaslessServiceConfig(
        appVersionProvider: AppVersionProvider,
        authProvider: AuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return GaslessTxService(
            authProvider = authProvider,
            appVersionProvider = appVersionProvider,
            appInfoProvider = appInfoProvider,
        )
    }
}