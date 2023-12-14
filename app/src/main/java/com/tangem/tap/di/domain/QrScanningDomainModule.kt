package com.tangem.tap.di.domain

import com.tangem.feature.qr_scanning.repo.QrScannedEventsRepository
import com.tangem.feature.qr_scanning.usecase.ListenToQrScanUseCase
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
    fun provideListenToQrScanUseCase(repository: QrScannedEventsRepository): ListenToQrScanUseCase {
        return ListenToQrScanUseCase(repository)
    }
}
