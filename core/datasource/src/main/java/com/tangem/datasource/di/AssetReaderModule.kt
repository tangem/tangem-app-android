package com.tangem.datasource.di

import android.content.Context
import com.tangem.datasource.asset.reader.AndroidAssetReader
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AssetReaderModule {

    @Singleton
    @Provides
    fun providesAsserReader(
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): AssetReader {
        return AndroidAssetReader(context.assets, dispatchers)
    }
}