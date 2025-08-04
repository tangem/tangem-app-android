package com.tangem.sdk.api.di

import android.content.Context
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.operations.attestation.CardArtworksProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardSdkModule {

    @Provides
    @Singleton
    fun provideCardArtworksProvider(
        sdkRepository: CardSdkConfigRepository,
        @ApplicationContext context: Context,
    ): CardArtworksProvider {
        return CardArtworksProvider(
            tangemApiBaseUrlProvider = { sdkRepository.sdk.config.tangemApiBaseUrl },
            artworksDirectory = File(
                context.getExternalFilesDir(null) ?: context.filesDir,
                "card_artworks",
            ).also { it.mkdirs() },
        )
    }
}