package com.tangem.tap.di.core

import android.content.Context
import com.tangem.core.ui.coil.ImagePreloader
import com.tangem.tap.common.images.DefaultImagePreloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImagePreloader {
        return DefaultImagePreloader(context)
    }
}