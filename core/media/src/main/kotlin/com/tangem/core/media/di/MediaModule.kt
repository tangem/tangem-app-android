package com.tangem.core.media.di

import com.tangem.core.media.DefaultLoopingVideoControllerFactory
import com.tangem.core.media.DefaultOneShotVideoControllerFactory
import com.tangem.core.media.DefaultSoundClipFactory
import com.tangem.core.media.LoopingVideoController
import com.tangem.core.media.OneShotVideoController
import com.tangem.core.media.SoundClip
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

    @Binds
    @Singleton
    fun bindOneShotVideoControllerFactory(factory: DefaultOneShotVideoControllerFactory): OneShotVideoController.Factory

    @Binds
    @Singleton
    fun bindSoundClipFactory(factory: DefaultSoundClipFactory): SoundClip.Factory
}