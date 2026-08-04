package com.tangem.datasource.di

import com.tangem.core.remote.config.ApiConfig
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
    @StringKey(Express.KEY)
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
    @StringKey(StakeKit.KEY)
    fun provideStakeKitConfig(stakeKitAuthProvider: StakeKitAuthProvider): ApiConfig {
        return StakeKit(stakeKitAuthProvider)
    }

    @Provides
    @IntoMap
    @StringKey(P2PEthPool.KEY)
    fun provideP2PEthPoolConfig(p2pAuthProvider: P2PEthPoolAuthProvider): ApiConfig {
        return P2PEthPool(p2pAuthProvider)
    }

    @Provides
    @IntoMap
    @StringKey(TangemTech.KEY)
    fun provideTangemTechConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return TangemTech(
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoMap
    @StringKey(News.KEY)
    fun provideNewsConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return News(
            appInfoProvider = appInfoProvider,
            authProvider = authProvider,
        )
    }

    @Provides
    @IntoMap
    @StringKey(YieldSupply.KEY)
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
    @StringKey(BlockAid.KEY)
    fun provideBlockAidConfig(environmentConfig: EnvironmentConfig): ApiConfig {
        return BlockAid(environmentConfig)
    }

    @Provides
    @IntoMap
    @StringKey(MoonPay.KEY)
    fun provideMoonPayConfig(): ApiConfig {
        return MoonPay()
    }

    @Provides
    @IntoMap
    @StringKey(GaslessTxService.KEY)
    fun provideGaslessServiceConfig(authProvider: AuthProvider, appInfoProvider: AppInfoProvider): ApiConfig {
        return GaslessTxService(
            authProvider = authProvider,
            appInfoProvider = appInfoProvider,
        )
    }

    @Provides
    @IntoMap
    @StringKey(SurveySparrow.KEY)
    fun provideSurveySparrowConfig(environmentConfig: EnvironmentConfig): ApiConfig {
        return SurveySparrow(environmentConfig)
    }

    @Provides
    @IntoMap
    @StringKey(Auth.KEY)
    fun provideAuthConfig(): ApiConfig {
        return Auth()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketWeb.KEY)
    fun providePolymarketWebConfig(): ApiConfig {
        return PolymarketWeb()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketRelayer.KEY)
    fun providePolymarketRelayerConfig(): ApiConfig {
        return PolymarketRelayer()
    }

    @Provides
    @IntoMap
    @StringKey(PolymarketClob.KEY)
    fun providePolymarketClobConfig(): ApiConfig {
        return PolymarketClob()
    }
}