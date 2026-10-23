package com.tangem.core.media.di

import com.tangem.core.media.DefaultLoopingVideoControllerFactory
import com.tangem.core.media.LoopingVideoController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MediaModule {

    @Binds
    @Singleton
    fun bindLoopingVideoControllerFactory(factory: DefaultLoopingVideoControllerFactory): LoopingVideoController.Factory
}