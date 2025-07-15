package com.tangem.sdk.api.di

import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.operations.attestation.CardArtworksProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardSdkModule {

    @Provides
    @Singleton
    fun provideCardArtworksProvider(sdkRepository: CardSdkConfigRepository): CardArtworksProvider {
        return CardArtworksProvider(
            tangemApiBaseUrlProvider = { sdkRepository.sdk.config.tangemApiBaseUrl },
            secureStorage = sdkRepository.sdk.secureStorage,
        )
    }
}