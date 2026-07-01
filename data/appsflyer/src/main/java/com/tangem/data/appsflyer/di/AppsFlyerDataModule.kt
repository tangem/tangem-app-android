package com.tangem.data.appsflyer.di

import com.tangem.data.appsflyer.DefaultAppsFlyerRepository
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.appsflyer.usecase.IsReferralInstallUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppsFlyerDataModule {

    @Binds
    @Singleton
    fun bindAppsFlyerRepository(repository: DefaultAppsFlyerRepository): AppsFlyerRepository

    companion object {

        @Provides
        fun provideClearAppsFlyerDeeplinkUseCase(
            appsFlyerRepository: AppsFlyerRepository,
        ): ClearAppsFlyerDeeplinkUseCase {
            return ClearAppsFlyerDeeplinkUseCase(appsFlyerRepository)
        }

        @Provides
        fun provideIsReferralInstallUseCase(
            appsFlyerRepository: AppsFlyerRepository,
        ): IsReferralInstallUseCase {
            return IsReferralInstallUseCase(appsFlyerRepository)
        }
    }
}