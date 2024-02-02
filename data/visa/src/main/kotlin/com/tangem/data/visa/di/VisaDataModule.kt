package com.tangem.data.visa.di

import com.tangem.data.visa.DummyVisaRepository
import com.tangem.domain.visa.repository.VisaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object VisaDataModule {

    @Provides
    @Singleton
    fun provideVisaRepository(): VisaRepository {
        return DummyVisaRepository()
    }
}