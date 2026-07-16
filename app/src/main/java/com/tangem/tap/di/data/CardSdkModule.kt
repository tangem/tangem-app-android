package com.tangem.tap.di.data

import android.content.Context
import com.tangem.data.card.sdk.CardSdkOwner
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.operations.attestation.CardArtworksProvider
import com.tangem.tap.data.DefaultCardSdkProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CardSdkModule {

    @Binds
    @Singleton
    fun provideCardSdkProvider(defaultCardSdkProvider: DefaultCardSdkProvider): CardSdkProvider

    @Binds
    @Singleton
    fun providerCardSdkLifecycleObserver(defaultCardSdkProvider: DefaultCardSdkProvider): CardSdkOwner

    companion object {

        @Provides
        @Singleton
        fun provideCardArtworksProvider(
            sdkRepository: CardSdkConfigRepository,
            @ApplicationContext context: Context,
        ): CardArtworksProvider {
            // Use internal storage (always mounted) instead of external files dir. External
            // storage can be transiently unavailable/unmounted or cleared after this singleton
            // is constructed, leaving the directory missing when the SDK later writes to it —
            // ArtworksStorage.store() opens a FileOutputStream without re-creating the parent,
            // which crashes with ENOENT. Artwork is only a cache, so internal storage is fine.
            val artworksDirectory = File(context.filesDir, "card_artworks").apply { mkdirs() }
            return CardArtworksProvider(
                tangemApiBaseUrlProvider = { sdkRepository.sdk.config.tangemApiBaseUrl },
                artworksDirectory = artworksDirectory,
            )
        }
    }
}