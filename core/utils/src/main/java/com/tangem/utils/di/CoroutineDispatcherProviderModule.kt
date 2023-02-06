package com.tangem.utils.di

import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CoroutineDispatcherProviderModule {

    @Binds
    fun bindCoroutineDispatcherProvider(
        coroutineDispatcherProvider: AppCoroutineDispatcherProvider,
    ): CoroutineDispatcherProvider
}
