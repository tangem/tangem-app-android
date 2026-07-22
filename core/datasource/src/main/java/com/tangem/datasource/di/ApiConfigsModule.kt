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
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
internal object ApiConfigsModule {

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.EXPRESS)
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
    @IntoMap
    @StringKey(ApiConfig.ID.STAKE_KIT)
    fun provideStakeKitConfig(stakeKitAuthProvider: StakeKitAuthProvider): ApiConfig {
        return StakeKit(stakeKitAuthProvider)
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.P2P_ETH_POOL)
    fun provideP2PEthPoolConfig(p2pAuthProvider: P2PEthPoolAuthProvider): ApiConfig {
        return P2PEthPool(p2pAuthProvider)
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.TANGEM_TECH)
    fun provideTangemTechConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemTech(
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.NEWS)
    fun provideNewsConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return News(
            appInfoProvider = appInfoProvider,
            authProvider = authProvider,
        )
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.YIELD_SUPPLY)
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
    @IntoMap
    @StringKey(ApiConfig.ID.TANGEM_PAY)
    fun provideTangemPayBffConfig(environmentConfig: EnvironmentConfig, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemPay.Bff(environmentConfig, appInfoProvider)
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.TANGEM_PAY_AUTH)
    fun provideTangemPayAuthConfig(environmentConfig: EnvironmentConfig, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemPay.Auth(environmentConfig, appInfoProvider)
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.BLOCK_AID)
    fun provideBlockAidConfig(environmentConfig: EnvironmentConfig): ApiConfig {
        return BlockAid(environmentConfig)
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.MOON_PAY)
    fun provideMoonPayConfig(): ApiConfig {
        return MoonPay()
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.GASLESS_TX_SERVICE)
    fun provideGaslessServiceConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return GaslessTxService(
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.SURVEY_SPARROW)
    fun provideSurveySparrowConfig(environmentConfig: EnvironmentConfig): ApiConfig {
        return SurveySparrow(environmentConfig)
    }

    @Provides
    @IntoMap
    @StringKey(ApiConfig.ID.AUTH)
    fun provideAuthConfig(): ApiConfig {
        return Auth()
    }
}