package com.tangem.features.earn.impl.di

import android.content.Context
import com.tangem.core.media.DiskRemoteClipCache
import com.tangem.core.media.RemoteClipCache
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
internal annotation class EarnHeroClip

@Module
@InstallIn(SingletonComponent::class)
internal object EarnHeroVideoModule {

    private const val CACHE_DIR_NAME = "earn-hero"
    private const val CACHE_MAX_BYTES = 16L * 1024L * 1024L

    @Provides
    @Singleton
    @EarnHeroClip
    fun provideHeroClipCache(
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): RemoteClipCache = DiskRemoteClipCache(
        context = context,
        dispatchers = dispatchers,
        dirName = CACHE_DIR_NAME,
        maxBytes = CACHE_MAX_BYTES,
    )
}