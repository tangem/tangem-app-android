package com.tangem.datasource.di

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.config.*
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
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
        environmentConfigStorage: EnvironmentConfigStorage,
        expressAuthProvider: ExpressAuthProvider,
        appVersionProvider: AppVersionProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig {
        return Express(
            environmentConfigStorage = environmentConfigStorage,
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
        environmentConfigStorage: EnvironmentConfigStorage,
        appVersionProvider: AppVersionProvider,
        authProvider: AuthProvider,
        appInfoProvider: AppInfoProvider,
    ): ApiConfig = YieldSupply(
        environmentConfigStorage = environmentConfigStorage,
        appVersionProvider = appVersionProvider,
        authProvider = authProvider,
        appInfoProvider = appInfoProvider,
    )

    @Provides
    @IntoSet
    fun provideTangemVisaConfig(appVersionProvider: AppVersionProvider): ApiConfig = TangemPay(appVersionProvider)

    @Provides
    @IntoSet
    fun provideTangemPayAuthConfig(appVersionProvider: AppVersionProvider): ApiConfig = TangemPayAuth(
        appVersionProvider,
    )

    @Provides
    @IntoSet
    fun provideBlockAidConfig(environmentConfigStorage: EnvironmentConfigStorage): ApiConfig {
        return BlockAid(environmentConfigStorage)
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