package com.tangem.sdk.api.di

import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.operations.attestation.CardArtworksProvider
import com.tangem.operations.attestation.OnlineCardVerifier
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
    fun provideOnlineCardVerifier(): OnlineCardVerifier = OnlineCardVerifier()

    @Provides
    @Singleton
    fun provideCardArtworksProvider(sdkRepository: CardSdkConfigRepository): CardArtworksProvider =
        CardArtworksProvider(sdkRepository.sdk.config.isTangemAttestationProdEnv)
}
