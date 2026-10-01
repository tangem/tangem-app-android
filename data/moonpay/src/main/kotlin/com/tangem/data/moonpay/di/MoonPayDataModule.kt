package com.tangem.data.moonpay.di

import com.tangem.data.moonpay.DefaultMoonPaySellRepository
import com.tangem.domain.moonpay.MoonPaySellRepository
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.grow.datasource.config.GrowEnvironmentConfig
import com.tangem.grow.datasource.moonpay.MoonPayApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MoonPayDataModule {

    @Provides
    @Singleton
    fun provideMoonPaySellRepository(
        api: MoonPayApi,
        growEnvironmentConfig: GrowEnvironmentConfig,
        getSelectedWalletUseCase: GetSelectedWalletUseCase,
    ): MoonPaySellRepository {
        return DefaultMoonPaySellRepository(
            api = api,
            apiKey = growEnvironmentConfig.moonPayApiKey,
            secretKey = growEnvironmentConfig.moonPayApiSecretKey,
            userWalletProvider = { getSelectedWalletUseCase.sync().getOrNull() },
        )
    }
}