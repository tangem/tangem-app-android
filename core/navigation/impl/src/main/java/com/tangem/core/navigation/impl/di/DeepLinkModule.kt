package com.tangem.core.navigation.impl.di

import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.impl.deeplink.DeepLinkHandlerRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DeepLinkModule {

    /** Declares the map features contribute their [DeepLinkHandler]s to, so it may also be empty. */
    @Multibinds
    fun deepLinkHandlers(): Map<String, DeepLinkHandler>

    companion object {

        @Provides
        @Singleton
        fun provideDeepLinkHandlerRegistry(
            deepLinkHandlers: Map<String, @JvmSuppressWildcards Provider<DeepLinkHandler>>,
        ): DeepLinkHandlerRegistry {
            return DeepLinkHandlerRegistry(deepLinkHandlers)
        }
    }
}