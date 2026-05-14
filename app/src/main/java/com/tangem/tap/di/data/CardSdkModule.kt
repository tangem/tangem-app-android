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
            return CardArtworksProvider(
                tangemApiBaseUrlProvider = { sdkRepository.sdk.config.tangemApiBaseUrl },
                artworksDirectory = File(
                    context.getExternalFilesDir(null) ?: context.filesDir,
                    "card_artworks",
                ).apply { mkdirs() },
            )
        }
    }
}