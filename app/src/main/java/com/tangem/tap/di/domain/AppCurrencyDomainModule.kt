package com.tangem.tap.di.domain

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object AppCurrencyDomainModule {

    @Provides
    @ViewModelScoped
    fun provideGetSelectedAppCurrencyUseCase(
        appCurrencyRepository: AppCurrencyRepository,
    ): GetSelectedAppCurrencyUseCase {
        return GetSelectedAppCurrencyUseCase(appCurrencyRepository)
    }
}