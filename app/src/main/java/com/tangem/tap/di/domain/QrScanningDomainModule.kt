package com.tangem.tap.di.domain

import com.tangem.feature.qrscanning.repo.QrScanningEventsRepository
import com.tangem.feature.qrscanning.usecase.ListenToQrScanningUseCase
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
}