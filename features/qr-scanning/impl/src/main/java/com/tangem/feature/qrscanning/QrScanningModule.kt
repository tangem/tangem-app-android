package com.tangem.feature.qrscanning

import com.tangem.feature.qrscanning.repo.DefaultQrScanningEventsRepository
import com.tangem.feature.qrscanning.repo.QrScanningEventsRepository
import com.tangem.feature.qrscanning.usecase.EmitQrScannedEventUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QrScanningModule {

    @Provides
    @Singleton
    fun provideQrScannedEventsRepository(): QrScanningEventsRepository {
        return DefaultQrScanningEventsRepository()
    }

    @Provides
    @Singleton
    fun provideEmitQrScannedEventUseCase(repository: QrScanningEventsRepository): EmitQrScannedEventUseCase {
        return EmitQrScannedEventUseCase(repository)
    }
}