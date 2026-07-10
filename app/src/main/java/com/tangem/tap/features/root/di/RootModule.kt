package com.tangem.tap.features.root.di

import com.tangem.tap.features.root.DefaultRootWarningContinuation
import com.tangem.tap.features.root.RootWarningContinuation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface RootModule {

    @Binds
    @Singleton
    fun bindRootWarningContinuation(impl: DefaultRootWarningContinuation): RootWarningContinuation
}