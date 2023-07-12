package com.tangem.tap.di.domain

import com.tangem.domain.card.ScanCardProcessor
import com.tangem.tap.domain.scanCard.DefaultScanCardProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardDomainModule {

    @Provides
    @Singleton
    fun provideScanCardUseCase(): ScanCardProcessor = DefaultScanCardProcessor()
}
