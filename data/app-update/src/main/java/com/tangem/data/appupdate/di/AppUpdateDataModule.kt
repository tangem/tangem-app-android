package com.tangem.data.appupdate.di

import com.tangem.data.appupdate.DefaultAppUpdateRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.appupdate.repository.AppUpdateRepository
import com.tangem.domain.appupdate.usecase.GetAppUpdateStateUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppUpdateDataModule {

    @Provides
    @Singleton
    fun provideAppUpdateRepository(
        tangemTechApi: TangemTechApi,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): AppUpdateRepository = DefaultAppUpdateRepository(
        tangemTechApi = tangemTechApi,
        appPreferencesStore = appPreferencesStore,
        dispatchers = dispatchers,
    )

    @Provides
    @Singleton
    fun provideGetAppUpdateStateUseCase(
        repository: AppUpdateRepository,
        appInfoProvider: AppInfoProvider,
    ): GetAppUpdateStateUseCase = GetAppUpdateStateUseCase(
        repository = repository,
        appInfoProvider = appInfoProvider,
    )
}