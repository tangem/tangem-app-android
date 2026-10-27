package com.tangem.utils.di

import com.tangem.utils.time.CurrentTimeProvider
import com.tangem.utils.time.SystemCurrentTimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface CurrentTimeProviderModule {

    @Binds
    fun bindCurrentTimeProvider(provider: SystemCurrentTimeProvider): CurrentTimeProvider
}