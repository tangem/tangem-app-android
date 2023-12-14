package com.tangem.feature.qrscanning

import com.tangem.feature.qrscanning.repo.DefaultQrScannedEventsRepository
import com.tangem.feature.qrscanning.repo.QrScannedEventsRepository
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
    fun provideQrScannedEventsRepository(): QrScannedEventsRepository {
        return DefaultQrScannedEventsRepository()
    }

    @Provides
    @Singleton
    fun provideEmitQrScannedEventUseCase(repository: QrScannedEventsRepository): EmitQrScannedEventUseCase {
        return EmitQrScannedEventUseCase(repository)
    }
}
