package com.tangem.domain.card.di

import com.tangem.TangemSdk
import com.tangem.domain.card.CardInteractor
import com.tangem.domain.card.impl.DefaultCardInteractor
import com.tangem.domain.card.repository.ScanCardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.DefineComponent
import dagger.hilt.InstallIn

@Module
@InstallIn(DomainCardComponent::class)
internal interface DomainCardModule {

    @Provides
    fun provideCardInteractor(
        scanCardRepository: ScanCardRepository,
        tangemSdk: TangemSdk,
    ): CardInteractor {
        return DefaultCardInteractor(scanCardRepository, tangemSdk)
    }
}

@DefineComponent
class DomainCardComponent
