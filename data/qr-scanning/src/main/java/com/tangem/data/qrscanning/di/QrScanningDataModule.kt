package com.tangem.data.qrscanning.di

import com.tangem.data.qrscanning.repository.DefaultQrScanningEventsRepository
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QrScanningDataModule {

    @Provides
    @Singleton
    fun provideQrScanningEventsRepository(): QrScanningEventsRepository {
        return DefaultQrScanningEventsRepository()
    }
}