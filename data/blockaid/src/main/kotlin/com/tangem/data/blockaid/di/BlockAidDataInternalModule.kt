package com.tangem.data.blockaid.di

import com.tangem.data.blockaid.BlockAidMapper
import com.tangem.data.blockaid.BlockAidRepository
import com.tangem.data.blockaid.DefaultBlockAidRepository
import com.tangem.datasource.api.common.blockaid.BlockAidApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BlockAidDataInternalModule {

    @Provides
    @Singleton
    fun provideRepository(api: BlockAidApi, dispatcherProvider: CoroutineDispatcherProvider): BlockAidRepository {
        return DefaultBlockAidRepository(
            api = api,
            dispatcherProvider = dispatcherProvider,
            mapper = BlockAidMapper(),
        )
    }
}