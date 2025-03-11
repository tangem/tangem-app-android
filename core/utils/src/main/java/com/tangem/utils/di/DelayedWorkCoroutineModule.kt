package com.tangem.utils.di

import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.DelayedWork
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DelayedWorkCoroutineModule {

    @Provides
    @Singleton
    @DelayedWork
    fun provideDelayedWorkCoroutineScope(coroutineDispatcherProvider: CoroutineDispatcherProvider): CoroutineScope {
        return CoroutineScope(SupervisorJob() + coroutineDispatcherProvider.io)
    }
}