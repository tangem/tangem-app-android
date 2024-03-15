package com.tangem.tap.di.domain

import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import com.tangem.domain.qrscanning.usecases.EmitQrScannedEventUseCase
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QrScanningDomainModule {

    @Provides
    @Singleton
    fun provideListenToQrScanUseCase(repository: QrScanningEventsRepository): ListenToQrScanningUseCase {
        return ListenToQrScanningUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideEmitQrScannedEventUseCase(repository: QrScanningEventsRepository): EmitQrScannedEventUseCase {
        return EmitQrScannedEventUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideParseQrCodeUseCase(repository: QrScanningEventsRepository): ParseQrCodeUseCase {
        return ParseQrCodeUseCase(repository)
    }
}