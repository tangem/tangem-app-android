package com.tangem.feature.qr_scanning

import com.tangem.feature.qr_scanning.repo.DefaultQrScannedEventsRepository
import com.tangem.feature.qr_scanning.repo.QrScannedEventsRepository
import com.tangem.feature.qr_scanning.usecase.EmitQrScannedEventUseCase
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
    fun provideQrScannedEventsRepository(): QrScannedEventsRepository {
        return DefaultQrScannedEventsRepository()
    }

    @Provides
    @Singleton
    fun provideEmitQrScannedEventUseCase(repository: QrScannedEventsRepository): EmitQrScannedEventUseCase {
        return EmitQrScannedEventUseCase(repository)
    }
}
